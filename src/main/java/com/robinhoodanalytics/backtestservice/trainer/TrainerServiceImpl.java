package com.robinhoodanalytics.backtestservice.trainer;

import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.models.Quote;
import com.robinhoodanalytics.backtestservice.quotes.QuoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

@Component("trainerService")
public class TrainerServiceImpl implements TrainerService {
    @Autowired
    QuoteService _quoteService;

    private static final Logger log = LoggerFactory.getLogger(BacktestServiceApplication.class);

    @Override
    public ResponseEntity train(String symbol, Date from, Date to) {
        try {
            log.info("START");

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            List<Quote> quotes = _quoteService.getHistoricalQuotes(symbol, from, to);

            if (quotes != null) {
                Deque<Quote> window = new ArrayDeque<>();
                long avgVolume = 0;
                long totalVolume = 0;
                for (Quote q: quotes) {
                    if (window.size() < 90) {
                        window.push(q);
                    } else if (window.size() > 90) {
                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                    } else if (window.size() == 90) {
                        totalVolume -= window.removeFirst().getVolume();
                        window.push(q);
                    }
                    totalVolume += q.getVolume();
                    avgVolume = totalVolume / window.size();
                    log.info("vol: {} {} {} {}", totalVolume, window.size(),  avgVolume, q.getVolume());
                }

                return new ResponseEntity<>(quotes, responseHeaders, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }


        } catch (RestClientException e) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }


}
