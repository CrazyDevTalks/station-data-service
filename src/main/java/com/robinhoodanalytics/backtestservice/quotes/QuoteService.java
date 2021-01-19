package com.robinhoodanalytics.backtestservice.quotes;

import com.robinhoodanalytics.backtestservice.models.IntradayQuote;
import com.robinhoodanalytics.backtestservice.models.Quote;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface QuoteService {
    ResponseEntity addIntradayQuotes(List<IntradayQuote> payload);
    List<Quote> getHistoricalQuotes(String symbol, Date from, Date to) throws RestClientException;
    ResponseEntity findIntradayQuotes(String symbol, LocalDateTime from, LocalDateTime to);
    ResponseEntity updateQuoteImpliedMove(String symbol, double impliedMove);
}
