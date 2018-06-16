package com.robinhoodanalytics.backtestservice.quotes;


import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.models.Quote;
import com.robinhoodanalytics.backtestservice.utils.DateParser;
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

    private static final Logger log = LoggerFactory.getLogger(BacktestServiceApplication.class);

    @Override
    public List<Quote> getHistoricalQuotes(String symbol, Date from, Date to)
            throws RestClientException {
        return retrieveQuotes(symbol, from, to);
    }

    public List<Quote> retrieveQuotes(String symbol, Date from, Date to)
            throws RestClientException
    {
        Date start = DateParser.toTradeDay(from, 0);
        Date end = DateParser.toTradeDay(to, 23);

        symbol = symbol.toUpperCase();
        List<Quote> quotes = quoteRepo.findBySymbolAndDateBetween(symbol, start, end);
        log.info("params: {} {} {}", symbol, start, end);

        long diff = Math.abs(to.getTime() - from.getTime());
        long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        log.info("DB results: {}, expected results: {} ", (double) quotes.size(), DateParser.estimateTradeDays(days));
        if (quotes.size() > 0) {
            log.info("Requested start date: {}, Found start date: {}, {}", start, quotes.get(0).getDate(), DateParser.compareTradeDays(start, quotes.get(0).getDate()));
            log.info("Requested end date: {}, Found end date: {}, {}", end, quotes.get(quotes.size() - 1).getDate(), DateParser.compareTradeDays(end, quotes.get(quotes.size() - 1).getDate()));
        }

        if (quotes.size() > 0 &&
            (double) quotes.size() - DateParser.estimateTradeDays(days) >= -50.0 &&
            DateParser.compareTradeDays(start, quotes.get(0).getDate()) >= 0 &&
            DateParser.compareTradeDays(DateParser.toTradeDay(end, 0), quotes.get(quotes.size() - 1).getDate()) <= 0) {
            log.info("Expected: {}, Found: {} ", DateParser.toTradeDay(end, 0), quotes.get(quotes.size() - 1).getDate());

            log.info("Using DB results");
            return quotes;
        }
        else {
            quoteRepo.delete(quotes);
            return addQuotes(symbol, diff);
        }
    }

    private List<Quote> addQuotes(String symbol, long timeRange) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String range;
        String interval = "1d";

        if (timeRange <= 5) {
            range = "5d";
        } else if (timeRange <= 30) {
            range = "1mo";
        } else if (timeRange <= 90) {
            range = "3mo";
        } else if (timeRange <= 365) {
            range = "1y";
        } else if (timeRange <= 730) {
            range = "2y";
        } else if (timeRange <= 1825) {
            range = "5y";
        } else {
            range = "10y";
        }

        HttpEntity<String> entity = new HttpEntity<>("{\"ticker\": \""+symbol+"\", \"interval\": \"" + interval + "\", \"range\": \"" + range + "\"}", headers);

        Quote[] response = restTemplate.postForObject("http://localhost:9000/api/quote", entity, Quote[].class);

        List<Quote> quotes = Arrays.asList(response);

        quoteRepo.save(quotes);
        log.info("Saved {} results", quotes.size());
        return quotes;
    }

    private List<Quote> updateQuotes(String symbol, List<Quote> existingQuotes, Date from, Date to) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd");

        HttpEntity<String> entity = new HttpEntity<>("{\"ticker\": \""+symbol+"\", \"start\": \"" + dt1.format(from) + "\", \"end\": \"" + dt1.format(to) + "\"}", headers);

        Quote[] response = restTemplate.postForObject("http://localhost:9000/api/quote", entity, Quote[].class);

        List<Quote> quotes = Arrays.asList(response);

        List<Quote> filteredQuotes = new ArrayList<>();

        for (Quote q: quotes) {
            boolean duplicate = false;
            for (int i = existingQuotes.size() - 1; i >= 0; i--) {
                log.info("looking: {} {}", existingQuotes.get(i).getDate(), q.getDate());

                if (existingQuotes.get(i).getDate().compareTo(q.getDate()) == 0) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                filteredQuotes.add(q);
            }
        }
        quoteRepo.save(filteredQuotes);
        return quotes;
    }
}