package com.merchantnottingham.littlejohnbacktestservice.quotes;


import com.merchantnottingham.littlejohnbacktestservice.LittlejohnBacktestServiceApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.config.ResourceNotFoundException;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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
    public ResponseEntity<List<Quote>> getHistoricalQuotes(String symbol, Date from, Date to)
            throws RestClientException {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        Date start = toMidnight(from, -1);
        Date end = toMidnight(to, 25);


        List<Quote> quotes = quoteRepo.findBySymbolAndDateBetween(symbol, start, end);
        log.info("params: {} {} {}", symbol, start, end);

        long diff = Math.abs(to.getTime() - from.getTime());
        long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        log.info("DB results: {}, expected results: {}, using DB results: {} ", (double) quotes.size(), days, (double) quotes.size() - ((days * 0.75) + 1.0) >= 0.0);

        if ((double) quotes.size() - ((days * 0.75) + 1.0) >= 0.0
//                &&
//                toMidnight(queried.get(0).getDate(), 0) == toMidnight(from, 0) &&
//                toMidnight(queried.get(queried.size() - 1).getDate(), 0) == toMidnight(to, 0)
                ) {

            return new ResponseEntity<>(quotes,
                    responseHeaders, HttpStatus.OK);
        } else {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd");

            HttpEntity<String> entity = new HttpEntity<>("{\"ticker\": \""+symbol+"\", \"start\": \"" + dt1.format(from) + "\", \"end\": \"" + dt1.format(to) + "\"}", headers);

            Quote[] response = restTemplate.postForObject("http://localhost:9000/api/quote", entity, Quote[].class);

            quotes = Arrays.asList(response);

            quoteRepo.save(quotes);

            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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
