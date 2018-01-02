package com.merchantnottingham.littlejohnbacktestservice.quotes;

import com.merchantnottingham.littlejohnbacktestservice.quotes.Quote;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public interface QuoteService {
    List<Quote> getHistoricalQuotes(String symbol, Calendar from, Calendar to) throws RestClientException;
}
