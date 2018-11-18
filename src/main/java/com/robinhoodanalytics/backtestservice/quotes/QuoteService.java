package com.robinhoodanalytics.backtestservice.quotes;

import com.robinhoodanalytics.backtestservice.models.IntradayQuote;
import com.robinhoodanalytics.backtestservice.models.Quote;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface QuoteService {
    ResponseEntity addIntradayQuotes(List<IntradayQuote> payload);
    List<Quote> getHistoricalQuotes(String symbol, Date from, Date to) throws RestClientException;
}
