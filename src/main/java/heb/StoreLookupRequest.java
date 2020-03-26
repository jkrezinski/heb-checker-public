package heb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreLookupRequest {

    private String address;
    private Boolean curbsideOnly;
    private Integer radius;
}
