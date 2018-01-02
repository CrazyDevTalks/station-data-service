package com.merchantnottingham.littlejohnbacktestservice.quotes;


import com.merchantnottingham.littlejohnbacktestservice.LittlejohnBacktestServiceApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

@Component("quoteService")
public class QuoteServiceImpl
    implements QuoteService
{
    @Autowired
    private QuoteRepository quoteRepo;

    private static final Logger log = LoggerFactory.getLogger(LittlejohnBacktestServiceApplication.class);

    @Override
    public List<Quote> getHistoricalQuotes(String symbol, Calendar from, Calendar to)
            throws RestClientException
    {
        List<Quote> quotesList = new ArrayList<>();

        List<Quote> queried = quoteRepo.findBySymbolAndDateBetween("goog", new GregorianCalendar(2017,12,28), new GregorianCalendar(2017,12,29));

        log.info("Querying quotes");
        queried.forEach(q -> {
            log.info("Quote: %s", q);
        });

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<String>("{\"ticker\": \"goog\", \"start\": \"2017-12-28\", \"end\": \"2017-12-29\"}", headers);

        Quote[] response = restTemplate.postForObject("http://localhost:9000/api/quote", entity, Quote[].class);

        quoteRepo.save(Arrays.asList(response));

        return Arrays.asList(response);
    }
}
