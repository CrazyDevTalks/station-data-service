package com.merchantnottingham.littlejohnbacktestservice.backtest;

import com.merchantnottingham.littlejohnbacktestservice.LittlejohnBacktestServiceApplication;
import com.merchantnottingham.littlejohnbacktestservice.quotes.Quote;
import com.merchantnottingham.littlejohnbacktestservice.quotes.QuoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/backtest")
public class BacktestController {
    @Autowired
    private QuoteService _quoteService;

    private static final Logger log = LoggerFactory.getLogger(LittlejohnBacktestServiceApplication.class);

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<List<Quote>> backtest()
    {
        List<Quote> quotes = null;
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders responseHeaders = new HttpHeaders();

        try {
            quotes =_quoteService.getHistoricalQuotes("goog", new GregorianCalendar(2017, 12, 28), new GregorianCalendar(2017, 12, 29));
        } catch (RestClientException e) {
            responseHeaders.setContentType(MediaType.TEXT_PLAIN);
            return new ResponseEntity<>(quotes,
                    responseHeaders, HttpStatus.SERVICE_UNAVAILABLE);
        }

        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(quotes,
                responseHeaders, HttpStatus.OK);
    }
}
