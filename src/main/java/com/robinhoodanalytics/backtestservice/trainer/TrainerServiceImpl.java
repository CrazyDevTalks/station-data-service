package com.robinhoodanalytics.backtestservice.trainer;

import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.models.AggregatedQuote;
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

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component("trainerService")
public class TrainerServiceImpl implements TrainerService {
    @Autowired
    QuoteService _quoteService;

    private static final Logger log = LoggerFactory.getLogger(BacktestServiceApplication.class);

    long totalVolume = 0;

    @Override
    public ResponseEntity train(String symbol, Date from, Date to) {
        try {
            log.info("START");

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            List<Quote> quotes = _quoteService.getHistoricalQuotes(symbol, from, to);
            AggregatedQuote[] items = new AggregatedQuote[quotes.size()];

            if (quotes != null) {
                Deque<Quote> window = new ArrayDeque<>();
                String name = "Run1";
                String[] featureNames = {"Volume Change", "Close", "Open", "High", "Low"};
                long avgVolume = 0;
                int ctr = 0;
                Quote previousQ = null;

                for (Quote q: quotes) {
                    Quote removed = null;
                    if (window.size() < 90) {
                        window.push(q);
                    } else if (window.size() > 90) {
                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                    } else if (window.size() == 90) {
                        removed = window.removeFirst();
                        window.push(q);
                    }

                    avgVolume = getAverageVolume(removed, q, window.size());

                    if (previousQ != null) {
                        double volChange = normalizeVolumeChange(avgVolume, q.getVolume());
                        double closeBinary = normalizePriceToBinary(previousQ.getClose(), q.getClose());
                        double openBinary = normalizePriceToBinary(previousQ.getOpen(), q.getOpen());
                        double highBinary = normalizePriceToBinary(previousQ.getHigh(), q.getHigh());
                        double lowBinary = normalizePriceToBinary(previousQ.getLow(), q.getLow());

                        double[] input = {volChange, closeBinary, openBinary, highBinary, lowBinary};
                        double[] previousOutput = {closeBinary};

                        int prevCtr = ctr - 1;
                        if (prevCtr > 0 && items[prevCtr] != null) {

                            items[prevCtr].set_output(previousOutput);
                        }

                        AggregatedQuote aq = new AggregatedQuote(q.getDate(), input, null);
                        items[ctr++] = aq;
                    }
                    previousQ = q;
                }

                return new ResponseEntity<>(items, responseHeaders, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }


        } catch (RestClientException e) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private long getAverageVolume(Quote removed, Quote current, int size) {
        if (removed != null) {
            totalVolume -= removed.getVolume();
        }
        totalVolume += current.getVolume();
        return totalVolume / size;
    }

    private double normalizeVolumeChange(long avgVol, long vol) {
        double change = (double)((float)vol/avgVol);
        return new BigDecimal(String.valueOf(change)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    private double normalizePriceToBinary(BigDecimal previousVal, BigDecimal newVal) {
        int comp = newVal.compareTo(previousVal);
        if (comp == -1) {
            return 0;
        }
        return comp;
    }
}
