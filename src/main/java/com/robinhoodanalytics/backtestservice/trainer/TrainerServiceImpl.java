package com.robinhoodanalytics.backtestservice.trainer;

import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.models.AggregatedQuote;
import com.robinhoodanalytics.backtestservice.models.Quote;
import com.robinhoodanalytics.backtestservice.quotes.AggregatedQuoteRepository;
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

    @Autowired
    AggregatedQuoteRepository _aggregatedQuoteRepo;

    private static final Logger log = LoggerFactory.getLogger(BacktestServiceApplication.class);

    @Override
    public ResponseEntity train(String symbol, Date from, Date to, boolean save) {
        try {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            List<Quote> quotes = _quoteService.getHistoricalQuotes(symbol, from, to);
            AggregatedQuote[] items = new AggregatedQuote[quotes.size() - 1];

            if (quotes != null) {
                Deque<Quote> window = new ArrayDeque<>();
                String name = "Run1";
                String[] featureNames = {"Volume Change", "Close", "Open", "High", "Low"};
                long avgVolume = 0;
                int ctr = 0;
                Quote previousQ = null;
                long volumeSum = 0;

                for (Quote q: quotes) {
                    //log.info("Curr Date: {} ", q.getDate().toString());

                    Quote removed = null;
                    if (window.size() < 90) {
                        window.push(q);
                    } else if (window.size() > 90) {
                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                    } else if (window.size() == 90) {
                        removed = window.removeLast();
                        window.push(q);
                    }

                    if (removed != null) {
                        volumeSum -= removed.getVolume();
                    }

                    volumeSum += q.getVolume();

                    avgVolume = getAverageVolume(volumeSum, window.size());

                    if (previousQ != null) {
                        double volChange = normalizeVolumeChange(avgVolume, q.getVolume());
                        double closeBinary = normalizePriceToBinary(previousQ.getClose(), q.getClose());
                        double openBinary = normalizePriceToBinary(previousQ.getOpen(), q.getOpen());
                        double highBinary = normalizePriceToBinary(previousQ.getHigh(), q.getHigh());
                        double lowBinary = normalizePriceToBinary(previousQ.getLow(), q.getLow());

                        double[] input = {volChange, closeBinary, openBinary, highBinary, lowBinary};
                        double[] previousOutput = {closeBinary};

                        int prevCtr = ctr - 1;
                        if (prevCtr >= 0 && items[prevCtr] != null) {
                            items[prevCtr].setOutput(previousOutput);
                            // log.info("Date: {} Close: {} Tomorrow Close: {}", previousQ.getDate().toString(), previousQ.getClose(), q.getClose());
                        }
                        // log.info("{} \t{}\t{}\t{}\t{}", window.size(), q.getVolume(), avgVolume, volChange, volumeSum);

                        // log.info("window {}", window.stream().map(Object::toString).collect(Collectors.joining(", ")));
                        // log.info("window {} - {}", window.getFirst(), window.getLast());

                        AggregatedQuote aq = new AggregatedQuote(q.getSymbol(), q.getDate(), input, null);
                        items[ctr++] = aq;
                    }
                    previousQ = q;
                }

                if (save && items.length > 0) {
                    List<AggregatedQuote> quoteList = Arrays.asList(items);
                    _aggregatedQuoteRepo.save(quoteList);
                }

                return new ResponseEntity<>(items, responseHeaders, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }


        } catch (RestClientException e) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private long getAverageVolume(long totalVolume, int size) {
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
