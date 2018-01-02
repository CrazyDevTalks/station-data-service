package com.merchantnottingham.littlejohnbacktestservice.quotes;

import com.merchantnottingham.littlejohnbacktestservice.quotes.Quote;
import org.springframework.web.client.RestClientException;

import java.util.Date;
import java.util.List;

public interface QuoteService {
    List<Quote> getHistoricalQuotes(String symbol, Date from, Date to) throws RestClientException;
}
