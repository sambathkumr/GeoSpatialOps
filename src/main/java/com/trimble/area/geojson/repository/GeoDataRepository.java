package com.trimble.area.geojson.repository;


import java.util.UUID;

import com.trimble.area.geojson.model.GeoData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GeoDataRepository extends MongoRepository<GeoData, UUID> {

  void deleteByAreaId(UUID id);
}
