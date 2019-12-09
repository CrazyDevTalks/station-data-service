package com.robinhoodanalytics.backtestservice.strategy;

import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.models.Quote;
import com.robinhoodanalytics.backtestservice.models.Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class FindResistance implements Strategy {
    private static final Logger log = LoggerFactory.getLogger(BacktestServiceApplication.class);

    private ArrayList<int[]> movingAveragePairs;
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
        findMatchingMovingAverageCrossOvers();

        setNextParameters(currentQuote, currentQuoteIndex);

        return null;
    }

    public void setParameters(Quote quote, int idx) {
        this.currentQuote = quote;
        this.currentQuoteIndex = idx;
    }

    private void setNextParameters(Quote quote, int idx) {
        addToQueue(quotesWindowIndices, idx, windowSize);
        addToQueue(highsHistory, quote.getHigh(), maxMovingAveragePeriod);
        addToQueue(lowsHistory, quote.getLow(), maxMovingAveragePeriod);
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

    private void findMatchingMovingAverageCrossOvers() {
        HashMap<Integer, BigDecimal> upperMovingAverageMap = new HashMap<>();

        int startIdx = this.upperResistanceIdx - maxMovingAveragePeriod;
        startIdx =  startIdx > 0 ? startIdx : 0;

        int counter = 0;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = startIdx; i < this.upperResistanceIdx; i++) {
            sum.add(this.quotes.get(i).getHigh());

            if (counter > 4) {
                upperMovingAverageMap.put(counter, sum.divide(new BigDecimal(counter),2, RoundingMode.HALF_EVEN));
            }
            counter++;
        }
    }

    private void findUpperLowerResistance() {
        Iterator<Integer> window = this.quotesWindowIndices.iterator();
        if (!window.hasNext()) {
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

            upperResistanceIdx = high;
            lowerResistanceIdx = low;
        }

    }
}
