package com.robinhoodanalytics.backtestservice.quotes;


import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.models.IntradayQuote;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.Optional;

@Component("quoteService")
public class QuoteServiceImpl
        implements QuoteService
{
    @Autowired
    private QuoteRepository quoteRepo;

    @Autowired
    private IntradayQuoteRepository intradayQuoteRepository;
    private static final Logger log = LoggerFactory.getLogger(BacktestServiceApplication.class);

    @Override
    public ResponseEntity addIntradayQuotes(List<IntradayQuote> payload){
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.TEXT_PLAIN);

        int len = payload.size();
        if (len > 99) {
            IntradayQuote first = payload.get(0);
            String symbol = first.symbol;
            IntradayQuote last = payload.get(len - 1);
            List<IntradayQuote> quotes = intradayQuoteRepository.findBySymbolAndDateBetween(symbol, first.date, last.date);

            if (quotes.size() >= len) {
                return new ResponseEntity<>("\"Quotes already exist\"", responseHeaders, HttpStatus.CONFLICT);
            } else {
                if (quotes.size() > 0) {
                    intradayQuoteRepository.deleteAll(quotes);
                }

                intradayQuoteRepository.saveAll(payload);
                return new ResponseEntity<>("\"Quotes added\"", responseHeaders, HttpStatus.OK);
            }
        }
        return new ResponseEntity<>("\"Not enough quotes to add\"", responseHeaders, HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity findIntradayQuotes(String symbol, LocalDateTime from, LocalDateTime to) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        symbol = symbol.toUpperCase();
        List<IntradayQuote> quotes = intradayQuoteRepository.findBySymbolAndDateBetween(symbol, from, to);
        log.info("Found: {}", quotes.size());
        return new ResponseEntity<>(quotes, responseHeaders, HttpStatus.OK);
    }

    @Override
    public List<Quote> getHistoricalQuotes(String symbol, Date from, Date to)
            throws RestClientException {
        return retrieveQuotes(symbol, from, to);
    }

    @Override
    public ResponseEntity updateQuoteImpliedMove(String symbol, double impliedMove)
            throws RestClientException
    {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        LocalDate date = LocalDate.now();
        Date targetDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());

        LocalDate pastvalue = date.minusDays(5);
        Date pastDate = Date.from(pastvalue.atStartOfDay(ZoneId.systemDefault()).toInstant());

        symbol = symbol.toUpperCase();
        List<Quote> quotes = quoteRepo.findBySymbolAndDateBetween(symbol, pastDate, targetDate);

        if (quotes.size() == 0) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Quote lastQuote = quotes.get(quotes.size() - 1);
        lastQuote.setImpliedMovement(impliedMove);
        quoteRepo.save(lastQuote);
        return new ResponseEntity<>(null, responseHeaders, HttpStatus.OK);
    }

    public List<Quote> retrieveQuotes(String symbol, Date from, Date to)
            throws RestClientException
    {
        Date start = DateParser.toTradeDay(from, 0, false);
        Date end = DateParser.toTradeDay(to, 23, true);
        Date currentDate = new Date();

        symbol = symbol.toUpperCase();
        List<Quote> quotes = quoteRepo.findBySymbolAndDateBetween(symbol, start, end);
        log.info("params: {} {} {}", symbol, start, end);

        long diff = Math.abs(currentDate.getTime() - from.getTime());
        long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        log.info("DB results: {}, expected results: {} ", (double) quotes.size(), DateParser.estimateTradeDays(days));
        if (quotes.size() > 0) {
            log.info("Requested start date: {}, Found start date: {}, {}", start, quotes.get(0).getDate(), DateParser.compareTradeDays(start, quotes.get(0).getDate()));
            log.info("Requested end date: {}, Found end date: {}, {}", end, quotes.get(quotes.size() - 1).getDate(), DateParser.compareTradeDays(end, quotes.get(quotes.size() - 1).getDate()));
        }

        if (quotes.size() > 0 && DateParser.compareTradeDays(start, quotes.get(0).getDate()) == 0 &&
            DateParser.compareTradeDays(DateParser.toTradeDay(end, 0, false), quotes.get(quotes.size() - 1).getDate()) == 0) {
            log.info("Expected: {}, Found: {} ", DateParser.toTradeDay(end, 0, false), quotes.get(quotes.size() - 1).getDate());

            log.info("Using DB results");
            return quotes;
        }
        else {
            if (quotes.size() > 0 &&
                    DateParser.compareTradeDays(start, quotes.get(0).getDate()) >= 0 && // First quote on or before expected date
                    DateParser.compareTradeDays(DateParser.toTradeDay(end, 0, false),
                        quotes.get(quotes.size() - 1).getDate()) > 0) { // Last quote is before the expected end date
                Date lastQuoteDate = quotes.get(quotes.size() - 1).getDate();

                long requestedTimeRange = Math.abs(from.getTime() - lastQuoteDate.getTime());
                List<Quote> requestedQuotes = this.getQuoteByRange(symbol, requestedTimeRange);

                if (requestedQuotes.size() > 0 &&
                        requestedQuotes.get(requestedQuotes.size() - 1).getDate().compareTo(lastQuoteDate) > 0) {
                    List<Quote> quotesToAdd = new ArrayList<>();

                    for (int i = 0; i < requestedQuotes.size(); i++) {
                        if (requestedQuotes.get(i).getDate().compareTo(lastQuoteDate) > 0) {
                            quotesToAdd.add(requestedQuotes.get(i));
                        }
                    }

                    log.info("Added {} new quotes", quotesToAdd.size());

                    quoteRepo.saveAll(quotesToAdd);
                }
                List<Quote> reretrieveQuotes = quoteRepo.findBySymbolAndDateBetween(symbol, start, end);
                return reretrieveQuotes;
            } else {
                List<Quote> allQuotes = quoteRepo.findBySymbol(symbol);

                quoteRepo.deleteAll(allQuotes);
                addQuotes(symbol, diff);
                return quoteRepo.findBySymbolAndDateBetween(symbol, start, end);
            }
        }
    }

    private List<Quote> getQuoteByRange(String symbol, long timeRange) {
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

        return Arrays.asList(response);
    }

    private List<Quote> addQuotes(String symbol, long timeRange) {
        List<Quote> quotes = getQuoteByRange(symbol, timeRange);
        quoteRepo.saveAll(quotes);

        log.info("Saved {} results", quotes.size());
        return quotes;
    }
}