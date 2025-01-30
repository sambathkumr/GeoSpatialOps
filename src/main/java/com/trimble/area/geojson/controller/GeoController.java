/**
 * Classification: Trimble Confidential.
 * Description: NetworkPartnerViewController
 *
 * @author Sambathkumar S
 * @since April 02 - 2024
 **/

package com.trimble.area.geojson.controller;

import java.util.UUID;

import com.trimble.area.geojson.model.Area;
import com.trimble.area.geojson.model.AreaPoint;
import com.trimble.area.geojson.model.GeoData;
import com.trimble.area.geojson.model.GeoJson;
import com.trimble.area.geojson.service.GeoJsonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/geo")
public class GeoController {

  private final GeoJsonService geoJsonService;

  @PostMapping("/merge")
  public GeoData mergeArea(final @RequestBody Area area) {
    return geoJsonService.mergePolygon(area);
  }

  @GetMapping("/geo-json/{id}")
  public GeoJson getGeoJsonById(@PathVariable UUID id) {
    return geoJsonService.getGeoJsonById(id);
  }

  @GetMapping("/centroid/{id}")
  public AreaPoint getCentroidById(@PathVariable UUID id) {
    return geoJsonService.getCentroidById(id);
  }

}

