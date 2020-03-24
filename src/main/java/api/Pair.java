package api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class Pair<T, K> {

    public Pair(T t, K k) {
        this.key = t;
        this.value = k;
    }

    private T key;
    private K value;
}
