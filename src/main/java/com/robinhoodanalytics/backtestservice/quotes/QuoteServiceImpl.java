package com.robinhoodanalytics.backtestservice.quotes;


import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.models.Quote;
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
            throws RestClientException {
        Date start = toTradeDay(from, 0);
        Date end = toTradeDay(to, 23);

        symbol = symbol.toUpperCase();
        List<Quote> quotes = quoteRepo.findBySymbolAndDateBetween(symbol, start, end);
        log.info("params: {} {} {}", symbol, start, end);

        long diff = Math.abs(to.getTime() - from.getTime());
        long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        log.info("DB results: {}, expected results: {} ", (double) quotes.size(), estimateTradeDays(days));
        if (quotes.size() > 0) {
            log.info("Requested start date: {}, Found start date: {}, {}", start, quotes.get(0).getDate(), compareTradeDays(start, quotes.get(0).getDate()));
            log.info("Requested end date: {}, Found end date: {}, {}", end, quotes.get(quotes.size() - 1).getDate(), compareTradeDays(end, quotes.get(quotes.size() - 1).getDate()));
        }

        if (quotes.size() > 0 && compareTradeDays(start, quotes.get(0).getDate()) >= 0 &&
                compareTradeDays(end, quotes.get(quotes.size() - 1).getDate()) > 0
                ) {
            log.info("Updating DB results");

            log.info("new start date: {}, end date: {}", quotes.get(quotes.size() - 1).getDate(), end);
            addQuotes(symbol, quotes.get(quotes.size() - 1).getDate(), end);

            return quotes;
        } else if (quotes.size() > 0 && (double) quotes.size() - estimateTradeDays(days) >= -10.0
                && compareTradeDays(start, quotes.get(0).getDate()) >= 0 &&
                compareTradeDays(end, quotes.get(quotes.size() - 1).getDate()) <= 0) {
            log.info("Using DB results");
            return quotes;
        }
        else {
            quoteRepo.delete(quotes);
            return addQuotes(symbol, from, to);
        }
    }

    private List<Quote> addQuotes(String symbol, Date from, Date to) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd");

        HttpEntity<String> entity = new HttpEntity<>("{\"ticker\": \""+symbol+"\", \"start\": \"" + dt1.format(from) + "\", \"end\": \"" + dt1.format(to) + "\"}", headers);

        Quote[] response = restTemplate.postForObject("http://localhost:9000/api/quote", entity, Quote[].class);

        List<Quote> quotes = Arrays.asList(response);

        quoteRepo.save(quotes);
        log.info("Saved {} results", quotes.size());
        return quotes;
    }

    private Date toTradeDay(Date date, int modifier) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND,0);
        cal.set(Calendar.HOUR_OF_DAY, modifier);

        if ((cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)) {
            cal.add(Calendar.DATE, -1);
        } else if ((cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)){
            cal.add(Calendar.DATE, -2);
        }
        return cal.getTime();
    }

    private Date standardizeDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND,0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        return cal.getTime();
    }
    private double estimateTradeDays(double days) {
        double workDaysPerWeek = 5.0 / 7.0;
        double holidays = 9.0;
        return Math.ceil((days * workDaysPerWeek) - holidays);
    }

    private int compareTradeDays(Date requestedDate, Date foundDate) {
        return standardizeDate(requestedDate).compareTo(standardizeDate(foundDate));
    }
}