package com.trimble.area.geojson.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "geodata")
@Data
public class GeoData {
  @Id
  private UUID id;
  private UUID areaId;
  @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
  private AreaGeometry geometry;
  private AreaPoint centerPoint;
  private Instant createdAtDateTime;
  private String errorMessage;
  private Long processTime;


}
