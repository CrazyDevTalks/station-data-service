package com.robinhoodanalytics.backtestservice.precog;

import com.robinhoodanalytics.backtestservice.models.Predictions;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PrecogRepository extends MongoRepository<Predictions, String>{
    @Query(value = "{ 'symbol' : ?0, 'date' : ?1}")
    List<Predictions> findBySymbolAndDate(String symbol, LocalDateTime from);
}
