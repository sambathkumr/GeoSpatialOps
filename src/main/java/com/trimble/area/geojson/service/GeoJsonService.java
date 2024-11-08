package com.trimble.area.geojson.service;


import java.util.List;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trimble.area.geojson.client.PcMilerClient;
import com.trimble.area.geojson.model.GeoDataTest;
import com.trimble.area.geojson.model.MapsPolygon;
import com.trimble.area.geojson.repository.GeoDataTestRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeoJsonService {

  private final PcMilerClient pcMilerClient;
  private final GeometryFactory geometryFactory = new GeometryFactory();
  private final WKTReader wktReader = new WKTReader(geometryFactory);
  private final ObjectMapper objectMapper;
  private final GeoDataTestRepository testRepository;
  @Value("${pc-miler.api.key}")
  private String token;


  public void mergeGeoJsons() {
    String statesCa ="AB,BC,MB,NB,NL,NS,NT,NU,ON,PE,QC,SK,YT";
    String statesUs =
        "AL,AK,AZ,AR,CA,CO,CT,DE,FL,GA,HI,ID,IL,IN,IA,KS,KY,LA,ME,MD,MA,MI,MN,MS,MO,MT,NE,NV,NH,NJ,NM,NY,NC,ND,OH,OK,OR,PA,RI,SC,SD,TN,TX,UT,VT,VA,WA,WV,WI,WY";
    var geoDataList = pcMilerClient.getStatesPolygon(token, statesUs);
    List<Geometry> geometries = geoDataList.parallelStream()
        .map(geoData -> {
          try {
            Geometry geometry = wktReader.read(geoData.getPolygon());
            if (!geometry.isValid()) {
              Geometry fixedGeometry = GeometryFixer.fix(geometry);
              if (fixedGeometry.isValid()) {
                geometry = fixedGeometry;
                System.out.println("Fixed geometry is valid");
              } else {
                System.out.println("Fixed geometry is still invalid");
              }
            }
            return geometry;
          } catch (ParseException e) {
            throw new RuntimeException(e);
          }
        }).collect(Collectors.toList());
    ForkJoinPool forkJoinPool = new ForkJoinPool();
    MergeTask mergeTask = new MergeTask(geometries);
    Geometry mergedGeo = forkJoinPool.invoke(mergeTask);
    if (!mergedGeo.isValid()) {
      Geometry fixedGeometry = GeometryFixer.fix(mergedGeo);
      if (fixedGeometry.isValid()) {
        mergedGeo = fixedGeometry;
      }
    }
    GeoJsonWriter geoJsonWriter = new GeoJsonWriter();
    String geoJson = geoJsonWriter.write(mergedGeo);
    GeoDataTest.Geo geo;
    try {
      geo = objectMapper.readValue(geoJson, GeoDataTest.Geo.class);
    } catch (Exception e) {
      throw new RuntimeException("Error converting JTS Geometry to GeoJSON", e);
    }
    saveGeoJson(geo, "US");

  }

  @SneakyThrows
  void createGeoJson() {
    String statesCa ="AB,BC,MB,NB,NL,NS,NT,NU,ON,PE,QC,SK,YT";
    String statesUs =
        "AL,AK,AZ,AR,CA,CO,CT,DE,FL,GA,HI,ID,IL,IN,IA,KS,KY,LA,ME,MD,MA,MI,MN,MS,MO,MT,NE,NV,NH,NJ,NM,NY,NC,ND,OH,OK,OR,PA,RI,SC,SD,TN,TX,UT,VT,VA,WA,WV,WI,WY";
    var geoDataList = pcMilerClient.getStatesPolygon(token, statesUs);
    for (MapsPolygon geoData : geoDataList) {
      System.out.println(geoData.getName() + geoData.getCode());
      Geometry geometry = wktReader.read(geoData.getPolygon());
      if (geometry.isValid()) {
        System.out.println("valid");
      } else {
        System.out.println("invalid");
        Geometry fixedGeometry = GeometryFixer.fix(geometry);
        if (fixedGeometry.isValid()) {
          geometry = fixedGeometry;
          System.out.println("Fixed geometry is valid");
        } else {
          System.out.println("Fixed geometry is still invalid");
        }
      }
      GeoJsonWriter geoJsonWriter = new GeoJsonWriter();
      String geoJson = geoJsonWriter.write(geometry);
      GeoDataTest.Geo geo;
      try {
        geo = objectMapper.readValue(geoJson, GeoDataTest.Geo.class);
      } catch (Exception e) {
        throw new RuntimeException("Error converting JTS Geometry to GeoJSON", e);
      }
      saveGeoJson(geo, geoData.getName());
    }
  }

  private void saveGeoJson(GeoDataTest.Geo geometry, String name) {
    GeoDataTest geoData = new GeoDataTest();
    geoData.setId(UUID.randomUUID());
    geoData.setName(name);
    geoData.setGeometry(geometry);
    try {
      testRepository.save(geoData);
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  private static class MergeTask extends RecursiveTask<Geometry> {
    private final List<Geometry> geometries;

    public MergeTask(List<Geometry> geometries) {
      this.geometries = geometries;
    }

    @Override
    protected Geometry compute() {
      if (geometries.size() == 1) {
        return geometries.get(0);
      } else if (geometries.size() == 2) {
        return geometries.get(0).union(geometries.get(1));
      } else {
        int mid = geometries.size() / 2;
        MergeTask leftTask = new MergeTask(geometries.subList(0, mid));
        MergeTask rightTask = new MergeTask(geometries.subList(mid, geometries.size()));
        leftTask.fork();
        Geometry rightResult = rightTask.compute();
        Geometry leftResult = leftTask.join();
        return leftResult.union(rightResult);
      }
    }
  }


}
