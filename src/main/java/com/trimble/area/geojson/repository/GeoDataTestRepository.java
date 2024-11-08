package com.trimble.area.geojson.repository;


import java.util.UUID;

import com.trimble.area.geojson.model.GeoDataTest;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GeoDataTestRepository extends MongoRepository<GeoDataTest, UUID> {

}
