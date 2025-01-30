package com.trimble.area.geojson.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trimble.area.geojson.client.PcMilerClient;
import com.trimble.area.geojson.model.Area;
import com.trimble.area.geojson.model.AreaGeometry;
import com.trimble.area.geojson.model.AreaPoint;
import com.trimble.area.geojson.model.GeoData;
import com.trimble.area.geojson.model.GeoJson;
import com.trimble.area.geojson.model.MapsPolygon;
import com.trimble.area.geojson.repository.GeoDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoJsonService {

  private static final int BATCH_SIZE = 100;
  private static final int THREAD_POOL_SIZE = 15;
  private static final int PRECISION_SCALE = 1000;
  private static final Set<String> usStates =
      Set.of("AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FL", "GA", "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
          "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD",
          "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY");
  private static final Set<String> caStates = Set.of("AB", "BC", "MB", "NB", "NL", "NS", "ON", "PE", "QC", "SK", "NT", "NU", "YT");
  private final PcMilerClient pcMilerClient;
  private final ObjectMapper objectMapper;
  private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PRECISION_SCALE), 4326);
  private final WKTReader wktReader = new WKTReader(geometryFactory);
  private final GeoDataRepository geoDataRepository;
  @Value("${pc-miler.api.key}")
  private String token;

  public GeoData mergePolygon(Area area) {
    long startTime = System.currentTimeMillis();
    try {
      return processPolygon(area, startTime);
    } catch (Exception ex) {
      log.error("Polygon - Error while merging polygons {}", ex.getMessage(), ex);
      long processTime = System.currentTimeMillis() - startTime;
      return savePolygon(area, null, null, ex.getMessage(), processTime);
    }
  }

  private GeoData processPolygon(Area area, long startTime) {
    var statesMap = processStatesCountries(area.getStates(), area.getCountries());
    var postalMap = processPostals(area.getPostalRanges(), area.getPostalCodes());

    List<MapsPolygon> includeList = new ArrayList<>();
    List<MapsPolygon> excludeList = new ArrayList<>();
    includeList.addAll(statesMap.getOrDefault(true, List.of()));
    includeList.addAll(postalMap.getOrDefault(true, List.of()));
    excludeList.addAll(statesMap.getOrDefault(false, List.of()));
    excludeList.addAll(postalMap.getOrDefault(false, List.of()));

    if (CollectionUtils.isNotEmpty(includeList)) {
      var geometry = combineIntoOneGeometry(includeList, excludeList);
      geometry = new GeometryPrecisionReducer(new PrecisionModel(PRECISION_SCALE)).reduce(geometry);
      if (!geometry.isValid()) {
        geometry = GeometryFixer.fix(geometry);
      }
      var Geometry = convertGeometry(geometry);
      AreaPoint areaPoint = new AreaPoint();
      if (Objects.nonNull(geometry.getCentroid())) {
        areaPoint.setLatitude(geometry.getCentroid().getY());
        areaPoint.setLongitude(geometry.getCentroid().getX());
      }
      long processTime = System.currentTimeMillis() - startTime;
      return savePolygon(area, Geometry, areaPoint, null, processTime);
    } else {
      log.warn("Polygon - include area is empty. Id - {} ", area.getId());
      deletePolygon(area);
    }
    return null;
  }

  public void deletePolygon(Area area) {
    geoDataRepository.deleteByAreaId(area.getId());
  }

  public GeoJson getGeoJsonById(UUID id) {
    var polygon = geoDataRepository.findById(id);
    if (polygon.isEmpty() || Objects.isNull(polygon.get().getGeometry())) {
      return null;
    }
    var polygonData = polygon.get();
    GeoJson.Feature feature = new GeoJson.Feature();
    feature.setGeometry(polygonData.getGeometry());
    GeoJson areaGeoJson = new GeoJson();
    areaGeoJson.add(feature);
    return areaGeoJson;
  }

  public AreaPoint getCentroidById(UUID id) {
    var polygon = geoDataRepository.findById(id);
    if (polygon.isEmpty() || Objects.isNull(polygon.get().getCenterPoint())) {
      return null;
    }
    return polygon.get().getCenterPoint();
  }

  private GeoData savePolygon(Area area, AreaGeometry geometry, AreaPoint point, String message, Long processTime) {
    var areaPolygon = new GeoData();
    areaPolygon.setId(UUID.randomUUID());
    areaPolygon.setAreaId(area.getId());
    areaPolygon.setGeometry(geometry);
    areaPolygon.setErrorMessage(message);
    areaPolygon.setCreatedAtDateTime(Instant.now());
    areaPolygon.setProcessTime(processTime);
    areaPolygon.setCenterPoint(point);
    try {
      log.info("Polygon - Saving Geometries");
      deletePolygon(area);
      return geoDataRepository.save(areaPolygon);
    } catch (Exception ex) {
      log.error("Polygon - Error while saving polygons {}", ex.getMessage(), ex);
      handleSaveError(areaPolygon, ex.getMessage());
    }
    return null;
  }

  private void handleSaveError(GeoData areaPolygon, String errorMessage) {
    areaPolygon.setErrorMessage(errorMessage);
    areaPolygon.setGeometry(null);
    try {
      geoDataRepository.save(areaPolygon);
    } catch (Exception ex) {
      log.error("Polygon - Error while saving error polygon: {}", ex.getMessage(), ex);
    }
  }

  private Map<Boolean, List<MapsPolygon>> processPostals(List<Area.PostalRange> postalRanges, List<Area.PostalCode> postalCodes) {
    if (CollectionUtils.isEmpty(postalRanges) && CollectionUtils.isEmpty(postalCodes)) {
      return Map.of();
    }
    Set<String> includePostal = new HashSet<>();
    Set<String> excludePostal = new HashSet<>();

    postalRanges.stream()
        .filter(postal -> "US".equalsIgnoreCase(postal.getCountry().getCode()))
        .forEach(postal -> {
          var range = generatePostalRange(postal.getStartRange(), postal.getEndRange());
          if (postal.getInclude()) {
            includePostal.addAll(range);
          } else {
            excludePostal.addAll(range);
          }
        });

    postalCodes.stream()
        .filter(postal -> "US".equalsIgnoreCase(postal.getCountry().getCode()))
        .forEach(postal -> {
          if (postal.getInclude()) {
            includePostal.add(postal.getCode());
          } else {
            excludePostal.add(postal.getCode());
          }
        });

    return Map.of(
        true, processPostal(new ArrayList<>(includePostal)),
        false, processPostal(new ArrayList<>(excludePostal))
    );
  }

  private List<String> generatePostalRange(String startRange, String endRange) {
    int start = Integer.parseInt(startRange);
    int end = Integer.parseInt(endRange);
    int length = startRange.length();

    return IntStream.rangeClosed(start, end)
        .mapToObj(i -> String.format("%0" + length + "d", i))
        .collect(Collectors.toList());
  }

  private List<MapsPolygon> processPostal(List<String> postalCodes) {
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    List<CompletableFuture<List<MapsPolygon>>> futures = Stream.iterate(0, n -> n + BATCH_SIZE)
        .limit((postalCodes.size() + BATCH_SIZE - 1) / BATCH_SIZE)
        .map(start -> CompletableFuture.supplyAsync(() -> getPostalCodePolygons(
            String.join(",", postalCodes.subList(start, Math.min(start + BATCH_SIZE, postalCodes.size())))
        ), executor)).toList();

    var result = futures.stream()
        .map(CompletableFuture::join)
        .flatMap(List::stream)
        .collect(Collectors.toList());

    executor.shutdown();
    return result;
  }

  private List<MapsPolygon> getPostalCodePolygons(String postalCodes) {
    try {
      return pcMilerClient.getPostalCodePolygon(token, postalCodes);
    } catch (Exception ex) {
      log.error("Polygon - Error while getting postal code polygon {} : {}", postalCodes, ex.getMessage());
      return List.of();
    }
  }

  private Map<Boolean, List<MapsPolygon>> processStatesCountries(List<Area.State> states, List<Area.Country> countries) {
    if (CollectionUtils.isEmpty(states) && CollectionUtils.isEmpty(countries)) {
      return Map.of();
    }

    Set<String> includeStates = new HashSet<>();
    Set<String> excludeStates = new HashSet<>();
    Set<String> stateCodes = new HashSet<>();

    if (CollectionUtils.isNotEmpty(countries)) {
      for (Area.Country country : countries) {
        String code = country.getCode().toUpperCase();
        Set<String> relevantStates = "US".equals(code) ? usStates : "CA".equals(code) ? caStates : Set.of();
        stateCodes.addAll(relevantStates);
        if (country.getInclude()) {
          includeStates.addAll(relevantStates);
        } /*else {
          // Skipping Country exclusion
          excludeStates.addAll(relevantStates);
        }*/
      }
    }

    if (CollectionUtils.isNotEmpty(states)) {
      for (Area.State state : states) {
        String country = state.getCountry().getCode().toUpperCase();
        if ("US".equals(country) || "CA".equals(country)) {
          String stateCode = state.getCode();
          stateCodes.add(stateCode);
          if (state.getInclude()) {
            includeStates.add(stateCode);
          } else {
            excludeStates.add(stateCode);
          }
        }
      }
    }

    stateCodes.removeAll(excludeStates);
    if (CollectionUtils.isEmpty(stateCodes)) {
      return Map.of();
    }
    List<MapsPolygon> polygons = getStatesPolygons(String.join(",", stateCodes));
    if (polygons.isEmpty()) {
      return Map.of();
    }

    Map<Boolean, List<MapsPolygon>> polygonsMap = new HashMap<>();
    for (MapsPolygon polygon : polygons) {
      String abbreviation = polygon.getAbbreviation();
      if (includeStates.contains(abbreviation)) {
        polygonsMap.computeIfAbsent(true, k -> new ArrayList<>()).add(polygon);
      } else if (excludeStates.contains(abbreviation)) {
        polygonsMap.computeIfAbsent(false, k -> new ArrayList<>()).add(polygon);
      }
    }

    return polygonsMap;
  }

  private List<MapsPolygon> getStatesPolygons(String states) {
    try {
      return pcMilerClient.getStatesPolygon(token, states);
    } catch (Exception ex) {
      log.error("Polygon - Error while getting states polygon {} : {}", states, ex.getMessage());
      return List.of();
    }
  }

  private AreaGeometry convertGeometry(Geometry geometry) {
    GeoJsonWriter geoJsonWriter = new GeoJsonWriter();
    String geoJson = geoJsonWriter.write(geometry);
    try {
      return objectMapper.readValue(geoJson, AreaGeometry.class);
    } catch (Exception e) {
      log.error("Polygon - Error converting Geometry- {}", e.getMessage());
      throw new RuntimeException("Error converting Geometry " + e.getMessage(), e);
    }
  }

  @SneakyThrows
  public Geometry combineIntoOneGeometry(List<MapsPolygon> includeList, List<MapsPolygon> excludeList) {
    if (CollectionUtils.isEmpty(includeList)) {
      return null;
    }

    Geometry combinedGeometry = unionGeometries(includeList);

    if (CollectionUtils.isNotEmpty(excludeList)) {
      Geometry excludeGeometry = unionGeometries(excludeList);
      log.info("Polygon - Difference Geometries");
      combinedGeometry = combinedGeometry.difference(excludeGeometry);
      if (!combinedGeometry.isValid()) {
        combinedGeometry = GeometryFixer.fix(combinedGeometry);
      }
    }

    return combinedGeometry;
  }

  private Geometry unionGeometries(List<MapsPolygon> polygons) {
    log.info("Polygon - Union Geometries");
    List<Geometry> geometries = polygons.parallelStream()
        .map(geoData -> {
          try {
            Geometry geometry = wktReader.read(geoData.getPolygon());
            if (!geometry.isValid()) {
              geometry = GeometryFixer.fix(geometry);
            }
            return geometry;
          } catch (ParseException e) {
            throw new RuntimeException(e);
          }
        }).collect(Collectors.toList());
    Geometry geometryCollection = geometryFactory.buildGeometry(geometries);
    Geometry combinedGeometry = geometryCollection.union();
    // Convert to single Geometry if it is a GeometryCollection
    if (combinedGeometry instanceof GeometryCollection) {
      combinedGeometry = combinedGeometry.buffer(0);
    }
    if (!combinedGeometry.isValid()) {
      combinedGeometry = GeometryFixer.fix(combinedGeometry);
    }
    return combinedGeometry;
  }

}
