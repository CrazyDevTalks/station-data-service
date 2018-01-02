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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component("quoteService")
public class QuoteServiceImpl
    implements QuoteService
{
    @Autowired
    private QuoteRepository quoteRepo;

    private static final Logger log = LoggerFactory.getLogger(LittlejohnBacktestServiceApplication.class);

    @Override
    public List<Quote> getHistoricalQuotes(String symbol, Date from, Date to)
            throws RestClientException {

        Date start = toMidnight(from, -1);
        Date end = toMidnight(to, 25);


        List<Quote> queried = quoteRepo.findBySymbolAndDateBetween(symbol, start, end);
        log.info("params: {} {} {}", symbol, start, end);

        log.info("Queried quotes: {}", queried.size());
        queried.forEach(q -> {
            log.info("Quote: {}", q);
        });

        long diff = to.getTime() - from.getTime();
        long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

        if (queried.size() >= days) {
            return queried;
        } else {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<String>("{\"ticker\": \"goog\", \"start\": \"2017-12-28\", \"end\": \"2017-12-29\"}", headers);

            Quote[] response = restTemplate.postForObject("http://localhost:9000/api/quote", entity, Quote[].class);

            List<Quote> quotes = Arrays.asList(response);
            quotes.forEach(q ->{
                        log.info("q: {}", q);
                    }
            );
            quoteRepo.save(quotes);

            return quotes;
        }
    }

    private Date toMidnight(Date date, int modifier) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND,0);
        cal.add(Calendar.HOUR_OF_DAY, modifier);

        return cal.getTime();
    }
}
