package com.trimble.area.geojson.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MapsPolygon {
    @JsonProperty("Name")
    private String name;

    @JsonProperty("Code")
    private String code;

    @JsonProperty("Polygon")
    private String polygon;

    @JsonProperty("Abbreviation")
    private String abbreviation;
}
