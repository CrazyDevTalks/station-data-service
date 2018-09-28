package com.robinhoodanalytics.backtestservice.quotes;

import com.robinhoodanalytics.backtestservice.models.AggregatedQuote;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface AggregatedQuoteRepository extends MongoRepository<AggregatedQuote, String>{
    @Query(value = "{ 'input.0' : {$gte : ?0, $lte: ?1 }}")
    List<AggregatedQuote> findByVolumeBetween(double min, double max);

    @Query(value = "{ 'input.0' : ?0 }")
    List<AggregatedQuote> findByVolume(double value);

    @Query(value = "{ 'symbol' : ?0, 'date' : {$gte : ?1, $lte: ?2 }}")
    List<AggregatedQuote> findBySymbolAndDateBetween(String symbol, LocalDate from, LocalDate to);
}
