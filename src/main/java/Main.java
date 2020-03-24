import api.Pair;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import heb.Response;
import heb.Store;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    private final HttpClient httpClient;
    private final AmazonSNS amazonSNSClient;

    private final String KEY = "PUT_YOUR_AWS_KEY_HERE";
    private final String PASS = "PUT_YOUR_AWS_SECRET_KEY_HERE";
    private final String SNS_TOPIC = "PUT_YOUR_AWS_SNS_TOPIC_URL_HERE";

    private final int TIMEOUT_MS = 60000;

    private final ExecutorService threadPool = Executors.newFixedThreadPool(40);
    private final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // These are the closet stores to me. I just checked the network tab on HEB.com when I typed in my zip code
    // and wrote these down
    private final Map<String, Instant> storeIds = new HashMap<>() {{
        put("373", null);
        put("591", null);
        put("495", null);
        put("388", null);
        put("580", null);
        put("673", null);
        put("659", null);
        put("236", null);
        put("31", null);
        put("265", null);
        put("24", null);
        put("269", null);
        put("476", null);
        put("479", null);
        put("696", null);
        put("237", null);
    }};

    private final String HEB_URL_FORMAT =
        "https://www.heb.com/commerce-api/v1/timeslot/timeslots?store_id=%s&days=15&fulfillment_type=pickup";

    public static void main(String[] args) {
        new Main().run();
    }

    public Main() {
        httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(KEY, PASS);

        amazonSNSClient = AmazonSNSClient.builder()
            .withRegion(Regions.US_EAST_1)
            .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
            .build();
    }

    private Callable<Pair<Store, Integer>> makeHebRequest(final HttpRequest httpRequest) {
        return () -> {
            try {
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                final String body = response.body();
                Response resp = MAPPER.readValue(body, Response.class);

                return new Pair<>(resp.getPickupStore(), resp.getItems().size());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return new Pair<>(null, null);
        };
    }

    private Runnable notifyChannel(Store store, Integer slots) {
        return () -> {
            final String message = String.format("Store ID %s at %s - %s has %d slots open!",
                store.getId(),
                store.getAddress1(),
                store.getPostalCode(),
                slots);

            Instant lastSent = storeIds.get(store.getId());

            if (lastSent == null || Instant.now().minusSeconds(3600 * 6).isAfter(lastSent)) {
                final PublishRequest publishRequest = new PublishRequest(SNS_TOPIC, message);
                amazonSNSClient.publish(publishRequest);

                storeIds.put(store.getId(), Instant.now());
            }
        };
    }

    private void run() {
        while (true) {
            try {

                // Start variables
                long start = System.currentTimeMillis();
                List<Future<Pair<Store, Integer>>> futures = new ArrayList<>();

                // Send out all requests
                for (String storeId : storeIds.keySet()) {

                    // Build request
                    HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(String.format(HEB_URL_FORMAT, storeId)))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                    futures.add(threadPool.submit(makeHebRequest(httpRequest)));
                }

                futures.stream()
                    .flatMap(future -> {
                        try {
                            return Stream.of(future.get());
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                            return Stream.empty();
                        }
                    })
                    .filter(pair -> Objects.nonNull(pair.getKey()) && Objects.nonNull(pair.getValue()))
                    .collect(Collectors.toList())
                    .forEach(pair -> {
                        if (pair.getValue() > 0) {
                            threadPool.execute(notifyChannel(pair.getKey(), pair.getValue()));
                        }
                    });

                System.out.println("Heartbeat");

                long end = System.currentTimeMillis();
                if (end - start < TIMEOUT_MS) {
                    // Don't spam them too hard, but try to be aggressive. This will call every store every TIMEOUT_MS
                    Thread.sleep((TIMEOUT_MS - (end - start)));
                }

            } catch (Exception e) {
                // Press F to pay respects
                e.printStackTrace();
            }
        }
    }
}
