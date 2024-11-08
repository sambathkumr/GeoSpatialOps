/**
 * Classification: Trimble Confidential.
 * Description: NetworkPartnerViewController
 *
 * @author Sambathkumar S
 * @since April 02 - 2024
 **/

package com.trimble.area.geojson.controller;

import com.trimble.area.geojson.service.GeoJsonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/geo-json")
public class GeojsonController {


  private final GeoJsonService geoJsonService;

  @GetMapping()
  public void geo() {
     geoJsonService.mergeGeoJsons();
  }

}

