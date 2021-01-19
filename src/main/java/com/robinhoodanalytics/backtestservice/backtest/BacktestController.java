package com.robinhoodanalytics.backtestservice.backtest;

import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.backtest.models.BacktestSettings;
import com.robinhoodanalytics.backtestservice.backtest.models.PrecogBacktestResults;
import com.robinhoodanalytics.backtestservice.models.BacktestSummary;
import com.robinhoodanalytics.backtestservice.models.IntradayQuote;
import com.robinhoodanalytics.backtestservice.trainer.TrainerService;
import com.robinhoodanalytics.backtestservice.models.Quote;
import com.robinhoodanalytics.backtestservice.quotes.QuoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/backtest")
public class BacktestController {
    @Autowired
    private QuoteService _quoteService;

    @Autowired
    private BacktestMainService _backtestMainService;

    @Autowired
    private TrainerService _trainerService;

    private static final Logger log = LoggerFactory.getLogger(BacktestServiceApplication.class);

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<List<Quote>> backtest(@RequestParam(value = "ticker") String symbol,
                                         @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date from,
                                         @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date to)
    {
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

    @RequestMapping(value = "/update-implied-move", method = RequestMethod.POST)
    ResponseEntity updateQuote(@RequestBody Map<String, Object> payload)
    {
        try {
            return _quoteService.updateQuoteImpliedMove((String) payload.get("symbol"), (double) payload.get("impliedMove"));
        } catch (RestClientException e) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }


    @RequestMapping(value = "/add/intradaydata", method = RequestMethod.POST)
    ResponseEntity addIntradayData(@RequestBody List<IntradayQuote> payload)
    {
        try {
            return _quoteService.addIntradayQuotes(payload);
        } catch (RestClientException e) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @RequestMapping(value = "/find/intraday", method = RequestMethod.GET)
    ResponseEntity findIntradayData(@RequestParam(value = "symbol") String symbol,
                                    @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date from,
                                    @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date to)
    {
        try {
            log.info("looking for: {} {} {}", symbol, from, to);
            LocalDateTime startDate = from.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime endDate = to.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            return _quoteService.findIntradayQuotes(symbol, startDate, endDate);
        } catch (RestClientException e) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @RequestMapping(
            value = "/train",
            method = RequestMethod.GET)
    ResponseEntity<List<Quote>> train(@RequestParam(value = "ticker") String symbol,
                                      @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date from,
                                      @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date to,
                                      @RequestParam boolean save,
                                      @RequestParam boolean useClosePrice)
    {
        try {
            return _trainerService.train(symbol, from, to, save, useClosePrice);
        } catch (RestClientException e) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @RequestMapping(
            value = "/train/find",
            method = RequestMethod.POST)
    ResponseEntity<List<Quote>> findTrainedData(@RequestBody Map<String, Object> payload)
    {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
            LocalDate from = LocalDate.parse((String) payload.get("from"), formatter);
            LocalDate to = LocalDate.parse((String) payload.get("to"), formatter);

            return _trainerService.findTrainingData((String) payload.get("symbol"), from, to, (Boolean) payload.get("save"));
        } catch (RestClientException e) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @RequestMapping(
            value = "/run",
            method = RequestMethod.POST)
    @ResponseBody
    public String executeSomeBacktest(@RequestParam("tickers") List<String> symbols,
                                      @RequestParam("strategy") String tradingStrategy,
                                      @RequestParam("cash") BigDecimal initialFund,
                                      @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date from,
                                      @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date to) 
    {
        switch(tradingStrategy.toLowerCase()) {
            case "buyandhold":
                _backtestMainService.buyAndHold(symbols, from, to, initialFund);
            break;
        }
        return "Get some Foos with Header";
    }

    @RequestMapping(
            value = "/strategy/mean-reversion/train",
            method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity meanReversionTrainer(@RequestParam("symbol") String symbol,
                                                                        @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date from,
                                                                        @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date to,
                                                                        @RequestParam(value = "d", required = false) BigDecimal deviation,
                                                                        @RequestParam("s") int shortTerm,
                                                                        @RequestParam("l") int longTerm
    ) {

        try {
            return ResponseEntity.ok(_backtestMainService.getMeanReversionResults(symbol, from, to, deviation, shortTerm, longTerm));
        } catch (Exception e) {
            log.error("Error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @RequestMapping(
            value = "/strategy/mean-reversion/chart",
            method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity meanReversionChart(@RequestParam("symbol") String symbol,
                                             @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date from,
                                             @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date to,
                                             @RequestParam("s") int shortTerm,
                                             @RequestParam("l") int longTerm
    ) {

        try {
            return ResponseEntity.ok(_backtestMainService.getMeanReversionTimeline(symbol, from, to, shortTerm, longTerm));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e);
        }
    }

    @RequestMapping(
            value = "/strategy/rnn",
            method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity rnnBacktest(@RequestParam("symbol") String symbol,
                                               @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date from,
                                               @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date to
    ) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        PrecogBacktestResults[] results = _backtestMainService.backtestRnn(symbol, from, to);

        return new ResponseEntity<>(results, responseHeaders, HttpStatus.OK);
    }

    @RequestMapping(
            value = "/strategy/hmm",
            method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity hmmBacktest() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        _trainerService.trainHmmModel();

        return new ResponseEntity<>(null, responseHeaders, HttpStatus.OK);
    }

    @RequestMapping(value = "strategy", method = RequestMethod.POST)
    ResponseEntity<BacktestSummary> backtestStrategy(@RequestBody BacktestSettings body)
    {
        try {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            BacktestSummary results = _backtestMainService.backtestStrategy(body.symbol, body.strategy,
                    body.from, body.to, body.settings);

            if (results != null) {
                return new ResponseEntity<BacktestSummary>(results, responseHeaders, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("Error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
