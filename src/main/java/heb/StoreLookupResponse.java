package heb;

import api.StoreDistance;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreLookupResponse {

    private List<StoreDistance> stores;
}
