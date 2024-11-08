package com.trimble.area.geojson;

import com.trimble.area.geojson.client.PcMilerClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(clients = {PcMilerClient.class})
public class GeoApplication {

  public static void main(String[] args) {
    SpringApplication.run(GeoApplication.class, args);
  }

}
