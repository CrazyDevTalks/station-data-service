package com.robinhoodanalytics.backtestservice.precog;

import com.robinhoodanalytics.backtestservice.models.Predictions;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PrecogRepository extends MongoRepository<Predictions, String>{
    @Query(value = "{ 'symbol' : ?0, 'modelName' : ?1, 'date' : ?2}")
    List<Predictions> findBySymbolAndModelNameAndDate(String symbol, String modelName, LocalDateTime from);
}
