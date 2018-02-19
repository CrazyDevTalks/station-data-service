package com.merchantnottingham.littlejohnbacktestservice.quotes;


import com.merchantnottingham.littlejohnbacktestservice.LittlejohnBacktestServiceApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

        Date start = toTradeDay(from, -1);
        Date end = toTradeDay(to, 25);

        symbol = symbol.toUpperCase();
        List<Quote> quotes = quoteRepo.findBySymbolAndDateBetween(symbol, start, end);
        log.info("params: {} {} {}", symbol, start, end);

        long diff = Math.abs(to.getTime() - from.getTime());
        long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        log.info("DB results: {}, expected results: {} ", (double) quotes.size(), days);

        if ((double) quotes.size() - ((days * 0.685) + 1.0) >= 0.0) {
            log.info("Using DB results");

            log.info("start date: {}, end date: {}", quotes.get(0).getDate(), quotes.get(quotes.size() - 1).getDate());
            return new ResponseEntity<>(quotes,
                    responseHeaders, HttpStatus.OK);
        } else if (quotes.size() > 0 && start.compareTo(toTradeDay(quotes.get(0).getDate(), 0)) >= 0 &&
                end.compareTo(toTradeDay(quotes.get(quotes.size() - 1).getDate(), 0)) > 0
                ) {
            log.info("Updating DB results");

            log.info("new start date: {}, end date: {}", toTradeDay(quotes.get(quotes.size() - 1).getDate(), 0), end);
            addQuotes(symbol, toTradeDay(quotes.get(quotes.size() - 1).getDate(), 0), end);

            return new ResponseEntity<>(quotes,
                    responseHeaders, HttpStatus.OK);
        }
        else {
            quoteRepo.delete(quotes);
            addQuotes(symbol, from, to);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private void addQuotes(String symbol, Date from, Date to) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd");

        HttpEntity<String> entity = new HttpEntity<>("{\"ticker\": \""+symbol+"\", \"start\": \"" + dt1.format(from) + "\", \"end\": \"" + dt1.format(to) + "\"}", headers);

        Quote[] response = restTemplate.postForObject("http://localhost:9000/api/quote", entity, Quote[].class);

        List<Quote> quotes = Arrays.asList(response);

        quoteRepo.save(quotes);
        log.info("Saved {} results", quotes.size());
    }

    private Date toTradeDay(Date date, int hourModifier) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND,0);
        cal.add(Calendar.HOUR_OF_DAY, hourModifier);

        if ((cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)) {
            cal.add(Calendar.DATE, -1);
        } else if ((cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)){
            cal.add(Calendar.DATE, -2);
        }

        return cal.getTime();
    }
}