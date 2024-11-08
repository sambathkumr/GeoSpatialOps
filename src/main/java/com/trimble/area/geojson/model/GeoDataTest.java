package com.trimble.area.geojson.model;

import java.util.List;
import java.util.UUID;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "geodata_test")
@Data
public class GeoDataTest {
  @Id
  private UUID id;

  private String name;

  @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
  private Geo geometry;

  @Data
  public static class Geo {
    private List<?> coordinates;
    private String type;
  }
}
