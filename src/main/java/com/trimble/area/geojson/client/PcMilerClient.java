/**
 * Classification: Trimble Confidential.
 * Description: SingleSearchSeedDataClient
 *
 * @author Hari Krishna M
 * @since May 17 - 2023
 **/

package com.trimble.area.geojson.client;

import java.util.List;
import java.util.Map;

import com.trimble.area.geojson.model.MapsPolygon;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(value = "pcMiler", url = "${pc-miler.api.url}")
public interface PcMilerClient {

  @GetMapping(value = "polygons/zip")
  List<MapsPolygon> getPostalCodePolygon(
      @RequestParam("authToken") String authToken,
      @RequestParam("zipcodes") String zipcodes
  );

  @GetMapping(value = "polygons/county")
  List<MapsPolygon> getCountyPolygon(
      @RequestParam("authToken") String authToken,
      @RequestParam("codes") String codes
  );

  @GetMapping(value = "polygons/state")
  List<MapsPolygon> getStatesPolygon(
      @RequestParam("authToken") String authToken,
      @RequestParam("states") String states
  );
}
