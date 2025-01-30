package com.trimble.area.geojson.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeoJson {

  private String type = "FeatureCollection";
  private List<Feature> features = new ArrayList<>();

  public GeoJson add(Feature feature) {
    this.features.add(feature);
    return this;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Feature {
    private String type = "Feature";
    private AreaGeometry geometry;
    private Map<String, String> properties = new HashMap<>();
  }
}
