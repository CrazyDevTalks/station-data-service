package com.robinhoodanalytics.backtestservice.strategy;

import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.models.Action;
import com.robinhoodanalytics.backtestservice.models.Quote;
import com.robinhoodanalytics.backtestservice.models.Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

class Payload {
    BigDecimal[] reals;
    int period;
    Payload(BigDecimal[] reals, int period) {
        this.reals = reals;
        this.period = period;
    }
}

public class FindResistance implements Strategy {
    private static final Logger log = LoggerFactory.getLogger(BacktestServiceApplication.class);

    private ArrayList<List<Integer>> upperResistanceMovingAveragePairs;
    private ArrayList<List<Integer>> lowerResistanceMovingAveragePairs;

    private List<Quote> quotes;

    private int windowSize;
    private int maxMovingAveragePeriod;
    private int minMovingAveragePeriod;
    private int currentQuoteIndex;
    private int upperResistanceIdx;
    private int lowerResistanceIdx;

    private Quote currentQuote;

    private Deque<Integer> quotesWindowIndices;
    private Deque<BigDecimal> highsHistory = new ArrayDeque<>();
    private Deque<BigDecimal> lowsHistory = new ArrayDeque<>();


    public FindResistance(List<Quote> quotes, int size, int minMovingAveragePeriod, int maxMovingAveragePeriod) {
        this.quotes = quotes;
        this.windowSize = size;
        this.quotesWindowIndices = new ArrayDeque<>();
        this.maxMovingAveragePeriod = maxMovingAveragePeriod;
        this.minMovingAveragePeriod = minMovingAveragePeriod;
    }

    @Override
    public Signal onTick(Date date) {
        findUpperLowerResistance();
        HashMap<BigDecimal, List<Integer>> upperMovingAverageMap = getMovingAverages(this.upperResistanceIdx, "high");
        HashMap<BigDecimal, List<Integer>> lowerMovingAverageMap = getMovingAverages(this.lowerResistanceIdx, "low");

//        log.info("upper resistance ma map: {} ", upperMovingAverageMap);
//
//        log.info("lower resistance ma map: {} ", lowerMovingAverageMap);

        ArrayList<List<Integer>> movingAveragePairsHigh = this.findMovingAverageCrossOver(upperMovingAverageMap);
        ArrayList<List<Integer>> movingAveragePairsLow = this.findMovingAverageCrossOver(lowerMovingAverageMap);

        if (movingAveragePairsHigh != null) {
            this.upperResistanceMovingAveragePairs = movingAveragePairsHigh;

            int[] pair = getPair(this.upperResistanceMovingAveragePairs);
            log.info("Using pair {}", pair);
            if (currentQuoteIndex - pair[0] > -1 && currentQuoteIndex - pair[1] > -1) {
                List<BigDecimal[]> reals = this.buildQuoteLists(pair[0], pair[1], currentQuoteIndex, quotes, "high");

                log.info("{}, {}", reals.get(0).length, reals.get(1).length);

                List<List<BigDecimal>> fastSma = this.getSimpleMovingAverage(reals.get(0), pair[0]);
                List<List<BigDecimal>> slowSma = this.getSimpleMovingAverage(reals.get(1), pair[1]);
                log.info("fastSma {}", fastSma);
                log.info("slowSma {}", slowSma);

            }
        }

        if (movingAveragePairsLow != null) {
            this.lowerResistanceMovingAveragePairs = movingAveragePairsLow;
        }

        setNextParameters(currentQuote, currentQuoteIndex);

        return new Signal(date, Action.INDETERMINANT);
    }

    public void setParameters(Quote quote, int idx) {
        this.currentQuote = quote;
        this.currentQuoteIndex = idx;
    }

    private void setNextParameters(Quote quote, int idx) {
        this.quotesWindowIndices = this.addToQueue(this.quotesWindowIndices, idx, windowSize);
        this.highsHistory = this.addToQueue(this.highsHistory, quote.getHigh(), maxMovingAveragePeriod);
        this.lowsHistory = this.addToQueue(this.lowsHistory, quote.getLow(), maxMovingAveragePeriod);
    }

    private int[] getPair(ArrayList<List<Integer>> pairs) {
        List<Integer> firstPairs = pairs.get(0);
        int[] pair = {firstPairs.get(0), firstPairs.get(firstPairs.size() - 1)};
        return pair;
    }

    private List<BigDecimal[]> buildQuoteLists(int fastMovingAveragePeriod, int slowMovingAveragePeriod, int currentIndex, List<Quote> quotes, String priceBar) {
        List<BigDecimal[]> reals = new ArrayList<>();

        BigDecimal[] fastPrices = new BigDecimal[fastMovingAveragePeriod];
        BigDecimal[] slowPrices = new BigDecimal[slowMovingAveragePeriod];

        int ctr = 0;
        if (slowMovingAveragePeriod > fastMovingAveragePeriod) {
            int idx = currentIndex;

            while(idx > 0 && ctr < slowMovingAveragePeriod) {
                BigDecimal price = this.getPrice(priceBar, quotes, idx);
                if (ctr < fastMovingAveragePeriod) {
                    fastPrices[ctr] = price;
                }
                slowPrices[ctr] = price;
                ctr++;
                idx--;
            }
        }
        reals.add(fastPrices);
        reals.add(slowPrices);

        return reals;
    }

    private List<List<BigDecimal>> getSimpleMovingAverage(BigDecimal[] reals, int period) {
        String apiUrl = "http://localhost:9000/api/backtest/sma";
        Payload body = new Payload(reals, period);

        RestTemplate restTemplate = new RestTemplate();

        HttpEntity<Payload> entity = new HttpEntity<>(body);

        log.info("Sending {}", body);

        ResponseEntity<List<List<BigDecimal>>> response = restTemplate.exchange(apiUrl,
                HttpMethod.POST, entity, new ParameterizedTypeReference<List<List<BigDecimal>>>() {
                });
        return response.getBody();
    }

    private <T> Deque<T> addToQueue(Deque<T> list, T item, int maxSize) {
        if (list.size() < maxSize) {
            list.push(item);
        } else {
            list.removeLast();
            list.push(item);
        }

        return list;
    }

    private BigDecimal getPrice(String priceBar, List<Quote> quotes, int i) {
        switch (priceBar) {
            case "high":
                return quotes.get(i).getHigh();
            case "low":
                return quotes.get(i).getLow();
            default:
                return quotes.get(i).getClose();
        }
    }

    private HashMap<BigDecimal, List<Integer>> getMovingAverages(int index, String priceBar) {
        HashMap<BigDecimal, List<Integer>> movingAverageMap = new HashMap<>();

        int startIdx = index - maxMovingAveragePeriod;
        startIdx =  startIdx > 0 ? startIdx : 0;

        int counter = 0;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = this.upperResistanceIdx; i > startIdx; i--) {
            BigDecimal price;
            switch (priceBar) {
                case "high":
                    price = this.quotes.get(i).getHigh();
                    break;
                case "low":
                    price = this.quotes.get(i).getLow();
                    break;
                default:
                    price = this.quotes.get(i).getClose();
                    break;
            }
            sum = sum.add(price);

            if (counter > this.minMovingAveragePeriod) {
                BigDecimal average = sum.divide(new BigDecimal(counter),1, RoundingMode.HALF_EVEN);
                List<Integer> indices = new ArrayList<>();

                if (movingAverageMap.containsKey(average)) {
                    indices = movingAverageMap.get(average);

                }

                indices.add(counter);
                movingAverageMap.put(average, indices);

            }
            counter++;
        }

        return movingAverageMap;
    }

    private void findUpperLowerResistance() {
        Iterator<Integer> window = this.quotesWindowIndices.iterator();
        if (window.hasNext()) {
            int high = window.next();
            int low = high;

            while(window.hasNext()) {
                int currentIdx = window.next();
                Quote currentQuote = this.quotes.get(currentIdx);
                if(currentQuote.getHigh().compareTo(currentQuote.getHigh()) > 0) {
                    high = currentIdx;
                }
                if(currentQuote.getLow().compareTo(currentQuote.getLow()) < 0) {
                    low = currentIdx;
                }
            }

            this.upperResistanceIdx = high;
            this.lowerResistanceIdx = low;
//            log.info("Found resistance: {} - {}",this.quotes.get(this.lowerResistanceIdx).getLow(),
//                    this.quotes.get(this.upperResistanceIdx).getHigh());
        }
    }

    private ArrayList<List<Integer>> findMovingAverageCrossOver(HashMap<BigDecimal, List<Integer>> movingAverages) {
        ArrayList<List<Integer>> foundPairs = new ArrayList<>();
        movingAverages.entrySet().forEach(entry->{
            if (this.hasPairs(entry.getValue())) {
                log.info("Found pair{ {} -> {}", entry.getKey(), entry.getValue());
                foundPairs.add(entry.getValue());
            }
        });
        return foundPairs.size() > 0 ? foundPairs: null;
    }

    private Boolean hasPairs(List<Integer> movingAverages) {
        return movingAverages.size() > 1;
    }
}
