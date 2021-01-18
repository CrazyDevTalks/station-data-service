package com.robinhoodanalytics.backtestservice.quotes;

import com.robinhoodanalytics.backtestservice.models.Quote;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Date;
import java.util.List;

public interface QuoteRepository extends MongoRepository<Quote, String>{

    @Query(value = "{ 'symbol' : ?0, 'date' : {$gte : ?1, $lte: ?2 }}")
    List<Quote> findBySymbolAndDateBetween(String symbol, Date from, Date to);

    @Query(value = "{ 'symbol' : ?0}")
    List<Quote> findBySymbol(String symbol);
}
