package heb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PickupFulfillmentResponse {

    private Store pickupStore;
    private List<Object> items;
}
