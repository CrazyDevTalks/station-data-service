package com.robinhoodanalytics.backtestservice.trainer;

import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.models.AggregatedQuote;
import com.robinhoodanalytics.backtestservice.models.Quote;
import com.robinhoodanalytics.backtestservice.models.Signal;
import com.robinhoodanalytics.backtestservice.quotes.AggregatedQuoteRepository;
import com.robinhoodanalytics.backtestservice.quotes.QuoteService;
import com.robinhoodanalytics.backtestservice.strategy.BbandMfi;
import com.robinhoodanalytics.backtestservice.utils.DateParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import smile.sequence.HMM;
import smile.stat.distribution.EmpiricalDistribution;

import java.math.BigDecimal;
import java.time.*;
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
    public List<Quote> sanitizeQuotes(String symbol, Date from, Date to) {
        return _quoteService.getHistoricalQuotes(symbol, from, to)
                .stream().map(e  -> {
                    Date newDate = DateParser.standardizeDate(e.getDate());
                    e.setDate(newDate);
                    return e;
                }).collect(Collectors.toList());
    }

    @Override
    public AggregatedQuote[] convertTrainingData(String symbol, Date from, Date to, boolean outputClosePrice) {
        List<Quote> quotes = this.sanitizeQuotes(symbol, from, to);

        if (quotes != null) {
            AggregatedQuote[] items = aggregateQuotes(quotes, outputClosePrice);

            return items;
        }
        return null;
    }

    @Override
    public ResponseEntity train(String symbol, Date from, Date to, boolean save, boolean outputClosePrice) {
        try {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);

            AggregatedQuote[] items = this.convertTrainingData(symbol, from, to, outputClosePrice);

            if (items != null) {
                if (save) {
                    List<AggregatedQuote> quoteList = Arrays.asList(items);

                    _aggregatedQuoteRepo.saveAll(quoteList);
                }

                return new ResponseEntity<>(items, responseHeaders, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (RestClientException e) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public ResponseEntity findTrainingData(String symbol, LocalDate from, LocalDate to, boolean save) {
        try {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);

            Date searchStartDate = Date.from(from.minusDays(10).atStartOfDay(ZoneId.of("-04:00")).toInstant());
            Date toDate = Date.from(to.atStartOfDay(ZoneId.of("-04:00")).toInstant());

            List<Quote> quotes = _quoteService.getHistoricalQuotes(symbol, searchStartDate, toDate);

            if (quotes != null) {
                AggregatedQuote[] items = aggregateQuotes(quotes, false);

                List<List<AggregatedQuote>> matchingHistorical = new ArrayList<>();
                if (items.length > 0) {

                    int len = items.length;
                    int ctr = 0;
                    int startIdx = -1;
                    int endIdx = len;

                    log.info("from: {} to: {}", items[0].getDate().toString(), items[items.length - 1].getDate().toString());

                    while (startIdx == -1 && ctr < len) {
                        LocalDate currentDate = items[ctr].getDate();

                        if (currentDate.compareTo(from) == 0) {
                            startIdx = ctr;
                        }
                        ctr++;
                    }

                    if (startIdx < 0) {
                        startIdx = 0;
                    }

                    AggregatedQuote[] examinedQuotes = Arrays.copyOfRange(items, startIdx, endIdx);
                    int examinedQuotesLen = examinedQuotes.length;
                    double[] dayOneInputs = examinedQuotes[0].getInput();
                    double d1 = dayOneInputs[0];

                    for (AggregatedQuote o : examinedQuotes) {
                        log.info("{}", o.toString());
                    }

                    List<AggregatedQuote> found = _aggregatedQuoteRepo.findByVolumeBetween(d1 - 0.05, d1 + 0.05);

                    matchingHistorical.add(Arrays.asList(examinedQuotes));

                    for (AggregatedQuote aq : found) {
                        LocalDate endDate = aq.getDate().plusDays(8);
                        List<AggregatedQuote> foundHistoricalAggregatedQuotes = _aggregatedQuoteRepo.findBySymbolAndDateBetween(aq.getSymbol(), aq.getDate(), endDate);

                        int foundAggregationsLen = foundHistoricalAggregatedQuotes.size();

                        if (foundAggregationsLen > examinedQuotesLen) {
                            double numMatches = 0;
                            double attributesTotalSeen = 0;
                            for (int j = 0; j < examinedQuotesLen; j++) {
                                attributesTotalSeen++;
                                AggregatedQuote pastQuote = foundHistoricalAggregatedQuotes.get(j);
                                double[] pastInputs = pastQuote.getInput();
                                AggregatedQuote currentQuote = examinedQuotes[j];
                                double[] currentInputs = currentQuote.getInput();

                                if (pastInputs[0] >= currentInputs[0] - 0.15 && pastInputs[0] <= currentInputs[0] + 0.15) {
                                    if (pastInputs[1] == currentInputs[1]) {
                                        numMatches++;
                                    }
                                }
                            }
                            if (numMatches / attributesTotalSeen >= 1.0) {
                                matchingHistorical.add(foundHistoricalAggregatedQuotes);
                            }
                        }
                    }
                }
                log.info("Number of matches: {}", matchingHistorical.size());
                return new ResponseEntity<>(matchingHistorical, responseHeaders, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (RestClientException e) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private AggregatedQuote[] aggregateQuotes(List<Quote> quotes, boolean outputClosePrice) {
        AggregatedQuote[] items = new AggregatedQuote[quotes.size() - 1];
        Deque<Quote> window = new ArrayDeque<>();
        long avgVolume = 0;
        int ctr = 0;
        Quote previousQ = null;
        long volumeSum = 0;

        for (Quote q : quotes) {
            //log.info("Curr Date: {} ", q.getDate().toString());

            Quote removed = null;
            if (window.size() < 90) {
                window.push(q);
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

                double[] input = {volChange, openBinary, closeBinary, highBinary, lowBinary};
                double[] previousOutput = {openBinary};
                if (outputClosePrice) {
                    previousOutput[0] = closeBinary;
                }

                int prevCtr = ctr - 1;
                if (prevCtr >= 0 && items[prevCtr] != null) {
                    items[prevCtr].setOutput(previousOutput);
                    // log.info("Date: {} Close: {} Tomorrow Close: {}", previousQ.getDate().toString(), previousQ.getClose(), q.getClose());
                }
                // log.info("{} \t{}\t{}\t{}\t{}", window.size(), q.getVolume(), avgVolume, volChange, volumeSum);

                // log.info("window {}", window.stream().map(Object::toString).collect(Collectors.joining(", ")));
                // log.info("window {} - {}", window.getFirst(), window.getLast());

                AggregatedQuote aq = new AggregatedQuote(q.getSymbol(), q.getDate().toInstant().atZone(ZoneId.of("-04:00")).toLocalDate(), input, null);
                items[ctr++] = aq;
            }
            previousQ = q;
        }

        return items;
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

    public void trainHmmModel() {
        double[] pi = {0.5, 0.5};
        double[][] a = {{0.8, 0.2}, {0.2, 0.8}};
        double[][] b = {{0.6, 0.4}, {0.4, 0.6}};
        log.info("learn");
        EmpiricalDistribution initial = new EmpiricalDistribution(pi);

        EmpiricalDistribution[] transition = new EmpiricalDistribution[a.length];
        for (int i = 0; i < transition.length; i++) {
            transition[i] = new EmpiricalDistribution(a[i]);
        }

        EmpiricalDistribution[] emission = new EmpiricalDistribution[b.length];
        for (int i = 0; i < emission.length; i++) {
            emission[i] = new EmpiricalDistribution(b[i]);
        }

        String[] symbols = {"open_down", "open_up"};
        String[][] sequences = new String[5000][];
        int[][] labels = new int[5000][];
        for (int i = 0; i < sequences.length; i++) {
            sequences[i] = new String[30 * ((int) Math.random() * 5 + 1)];
            labels[i] = new int[sequences[i].length];
            int state = (int) initial.rand();
            sequences[i][0] = symbols[(int) emission[state].rand()];
            labels[i][0] = state;
            for (int j = 1; j < sequences[i].length; j++) {
                state = (int) transition[state].rand();
                sequences[i][j] = symbols[(int) emission[state].rand()];
                labels[i][j] = state;
            }
        }

        HMM<String> hmm = new HMM(sequences, labels);
        log.info("{}", hmm);

        double[] pi2 = {0.55, 0.45};
        double[][] a2 = {{0.7, 0.3}, {0.15, 0.85}};
        double[][] b2 = {{0.45, 0.55}, {0.3, 0.7}};
        HMM<String> init = new HMM<>(pi2, a2, b2, symbols);
        HMM<String> result = init.learn(sequences, 100);
        log.info("results: {}", result);

        String[] o = {"open_up", "open_up", "open_down", "open_down", "open_down", "open_up", "open_up", "open_up", "open_down", "open_down", "open_down", "open_up", "open_up", "open_up", "open_up"};
        int[] prediction = init.predict(o);
        log.info("prediction: {}", prediction);
    }
}
