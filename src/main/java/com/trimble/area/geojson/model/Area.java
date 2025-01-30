package com.trimble.area.geojson.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Area {

  private UUID id;
  private String name;
  private String description;
  @CreatedDate
  private LocalDateTime createdAt;
  @CreatedBy
  private String createdBy;
  @LastModifiedDate
  private LocalDateTime updatedAt;
  @LastModifiedBy
  private String updatedBy;
  private List<City> cities;
  private List<PostalCode> postalCodes;
  private List<Country> countries;
  private List<PostalRange> postalRanges;
  private List<State> states;


  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class City extends GeoInclude {

    @Id
    private UUID id;
    private UUID areaId;
    private String name;
    private CodeName state;
    private CodeName country;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class State extends GeoInclude {

    @Id
    private UUID id;
    private UUID areaId;
    private String code;
    private String name;
    private CodeName country;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Country extends GeoInclude {
    @Id
    private UUID id;
    private UUID areaId;
    private String code;
    private String name;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class PostalCode extends GeoInclude {
    @Id
    private UUID id;
    private UUID areaId;
    private String code;
    private CodeName country;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class PostalRange extends GeoInclude {
    @Id
    private UUID id;
    private UUID areaId;
    private String startRange;
    private String endRange;
    private CodeName country;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CodeName {
    private String code;
    private String name;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class GeoInclude {

    private Boolean include;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private Long versionNumber;
  }

}