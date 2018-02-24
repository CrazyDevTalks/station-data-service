package com.merchantnottingham.littlejohnbacktestservice.quotes;

import com.merchantnottingham.littlejohnbacktestservice.models.Quote;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import java.util.Date;
import java.util.List;

public interface QuoteService {
    ResponseEntity<List<Quote>> getHistoricalQuotes(String symbol, Date from, Date to) throws RestClientException;
}
