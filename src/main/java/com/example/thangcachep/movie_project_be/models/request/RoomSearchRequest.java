package com.example.thangcachep.movie_project_be.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class RoomSearchRequest {

    private Integer page;

    private Integer limit;

    private Integer guests;

    private Double minPrice;

    private Double maxPrice;

    public void normalizeDefaults() {
        if (page  == null) page  = 0;
        if (limit == null) limit = 12;
    }
}
