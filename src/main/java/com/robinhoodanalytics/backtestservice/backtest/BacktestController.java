package com.robinhoodanalytics.backtestservice.backtest;

import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.models.Quote;
import com.robinhoodanalytics.backtestservice.quotes.QuoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.util.*;

@RestController
@RequestMapping("/backtest")
public class BacktestController {
    @Autowired
    private QuoteService _quoteService;

    private static final Logger log = LoggerFactory.getLogger(BacktestServiceApplication.class);

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<List<Quote>> backtest(@RequestParam(value = "ticker") String symbol,
                                         @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date from,
                                         @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date to)
    {
        try {
            return _quoteService.getHistoricalQuotes(symbol, from, to);
        } catch (RestClientException e) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
    @RequestMapping(
            value = "/info/csv",
            method = RequestMethod.GET)
    @ResponseBody
    public String getFoosWithHeaders(@RequestParam("ticker") String symbol,
                                     @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date from,
                                     @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date to) {
        return "Get some Foos with Header";
    }

        @RequestMapping(
                value = "/strategy/mean-reversion/train",
                method = RequestMethod.POST)
        @ResponseBody
        public String meanReversionTrainer(@RequestParam("ticker") String symbol,
                                           @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date from,
                                           @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date to,
                                           @RequestParam("shortStart") int shortTermStart,
                                           @RequestParam("longStart") int longTermStart,
                                           @RequestParam("shortEnd") int shortTermEnd,
                                           @RequestParam("longEnd") int longTermEnd

        ) {

        return "Get some Foos with Header";
        }
}
