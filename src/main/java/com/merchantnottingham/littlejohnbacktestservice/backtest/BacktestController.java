package com.merchantnottingham.littlejohnbacktestservice.backtest;

import com.merchantnottingham.littlejohnbacktestservice.LittlejohnBacktestServiceApplication;
import com.merchantnottingham.littlejohnbacktestservice.quotes.Quote;
import com.merchantnottingham.littlejohnbacktestservice.quotes.QuoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/backtest")
public class BacktestController {
    @Autowired
    private QuoteService _quoteService;

    private static final Logger log = LoggerFactory.getLogger(LittlejohnBacktestServiceApplication.class);

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<List<Quote>> backtest(@RequestParam(value = "ticker") String symbol,
                                         @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date from,
                                         @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date to
    )
    {
        try {
            return _quoteService.getHistoricalQuotes(symbol, from, to);
        } catch (RestClientException e) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
