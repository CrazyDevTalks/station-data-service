package com.robinhoodanalytics.backtestservice.quotes;

import com.robinhoodanalytics.backtestservice.models.IntradayQuote;
import com.robinhoodanalytics.backtestservice.models.Quote;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface IntradayQuoteRepository extends MongoRepository<IntradayQuote, String> {

    @Query(value = "{ 'symbol' : ?0, 'date' : {$gte : ?1, $lte: ?2 }}")
    List<IntradayQuote> findBySymbolAndDateBetween(String symbol, LocalDateTime from, LocalDateTime to);
}
