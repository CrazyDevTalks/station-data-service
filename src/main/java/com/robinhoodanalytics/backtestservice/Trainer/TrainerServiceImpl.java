package com.robinhoodanalytics.backtestservice.Trainer;

import com.robinhoodanalytics.backtestservice.models.Quote;
import com.robinhoodanalytics.backtestservice.quotes.QuoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Date;
import java.util.List;

@Component("trainerService")
public class TrainerServiceImpl implements TrainerService {

    QuoteService _quoteService;

    @Override
    public ResponseEntity train(String symbol, Date from, Date to) {
        try {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            List<Quote> quotes = _quoteService.getHistoricalQuotes(symbol, from, to);

            if (quotes != null) {
                return new ResponseEntity<>(quotes, responseHeaders, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }


        } catch (RestClientException e) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
