package com.robinhoodanalytics.backtestservice.backtest;

import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.models.*;
import com.robinhoodanalytics.backtestservice.quotes.QuoteService;
import com.robinhoodanalytics.backtestservice.strategy.BuyAndHold;
import com.robinhoodanalytics.backtestservice.utils.DateParser;
import com.robinhoodanalytics.backtestservice.utils.RollingAverage;
import com.robinhoodanalytics.backtestservice.utils.Statistics;
import org.apache.tomcat.jni.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component("backtestMainService")
public class BacktestMainServiceImpl
    implements BacktestMainService
{
    @Autowired
    QuoteService _quoteService;

    private static final Logger log = LoggerFactory.getLogger(BacktestServiceApplication.class);

    @Override
    public List<Signal> getMeanReversionTimeline(String symbol, Date from, Date to, int shortTerm, int longTerm, int bbandPeriod)
            throws Exception
    {
        return trainMeanReversion(symbol, from, to, shortTerm, longTerm, bbandPeriod);
    }

    @Override
    public BacktestSummary getMeanReversionResults(String symbol, Date from, Date to, BigDecimal deviation, int shortTerm, int longTerm, int bbandPeriod)
        throws Exception
    {
        List<Signal> signals = trainMeanReversion(symbol, from, to, shortTerm, longTerm, bbandPeriod);
        BacktestSummary summary = calculateReturns(signals, deviation);

        if (signals.size() > 0) {
            Signal lastSignal = signals.get(signals.size() - 1);
            summary.lastPrice = lastSignal.getClose();
            summary.lastVolume = lastSignal.getVolume();
            summary.recommendation = lastSignal.getAction();
        }

        return summary;
    }

    @Override
    public List<Signal> buyAndHold(List<String> symbols, Date from, Date to, BigDecimal initialFund)
    {
        List<StockRank> stocks = new ArrayList<>();
        for(String symbol: symbols) {
            stocks.add(new StockRank(symbol, 1.0));
        }
        BacktestContext context = new BacktestContext(stocks, initialFund);
        BuyAndHold buyHold = new BuyAndHold();

        buyHold.initialize(context);

        Map<String, List<Quote>> data = new HashMap<>();

        for(StockRank stock: stocks) {
            _quoteService.getHistoricalQuotes(stock.getSymbol(), from, to);
        }

        return new ArrayList<>();
    }

    private List<Signal> trainMeanReversion(String symbol,
                                           Date from,
                                           Date to,
                                           int shortTermWindow,
                                           int longTermWindow,
                                           int bbandPeriod) throws Exception
    {
        List<Quote> quotes = _quoteService.getHistoricalQuotes(symbol, from, to);

        RollingAverage shortTerm = new RollingAverage(shortTermWindow);
        RollingAverage longTerm = new RollingAverage(longTermWindow);
        RollingAverage bband = new RollingAverage(bbandPeriod);
        RollingAverage volumeWindow = new RollingAverage(longTermWindow);
        Deque<BigDecimal> recentVolumeChanges = new ArrayDeque<>();
        Deque<BigDecimal> recentPrices = new ArrayDeque<>();

        int longestPeriod = bbandPeriod > longTermWindow ? bbandPeriod : longTermWindow;

        List<Signal> results = new ArrayList<>();

        int preload = 0;

        for (Quote quote : quotes) {
            shortTerm.add(quote.getClose());
            longTerm.add(quote.getClose());
            bband.add(quote.getClose());

            volumeWindow.add(new BigDecimal(quote.getVolume()));
//            if (preload < longestPeriod) {
//                preload++;
//            } else {
//                BigDecimal shortAvg = shortTerm.getAverage();
//                BigDecimal longAvg = longTerm.getAverage();
                BigDecimal shortAvg = BigDecimal.ONE;
                BigDecimal longAvg = BigDecimal.ONE;

                //BigDecimal pctChange = Statistics.percentDifference(shortAvg, longAvg).abs();
                BigDecimal pctChange = BigDecimal.ONE;
                BigDecimal volumeChange = Statistics.percentChange(volumeWindow.getAverage(), new BigDecimal(quote.getVolume()));

                recentVolumeChanges = addToQueue(volumeChange, recentVolumeChanges, 10);

                recentPrices = addToQueue(quote.getClose(), recentPrices, 10);

                Action action = getDecision(quote, shortAvg, longAvg, recentVolumeChanges, recentPrices, bband.getSamples());

                Signal sig = new Signal(quote.getDate(), action,
                        pctChange, shortAvg, longAvg, volumeChange, quote.getClose(), quote.getVolume());
                results.add(sig);
//            }
        }
        return results;
    }

    private Action getDecision(Quote quote, BigDecimal avg30, BigDecimal avg90,
                                    Deque<BigDecimal> volumeChanges, Deque<BigDecimal> prices, BigDecimal[] bband) {
        return getMeanReversionDirection(quote,  avg30, avg90,
                volumeChanges, prices, bband);
    }

    private Action getMeanReversionDirection(Quote quote, BigDecimal avg30, BigDecimal avg90,
                                             Deque<BigDecimal> volumeChanges, Deque<BigDecimal> prices, BigDecimal[] bband) {
        Action recommendation = Action.INDETERMINANT;
        BigDecimal lastPrice = quote.getClose();
        BigDecimal previousPrice = null;
        int lowVolumeDays = 0;
        int highVolumeDays = 0;
        int downward = 0;
        int upward = 0;
        BigDecimal lowVolumeThreshold = new BigDecimal(-0.3);
        BigDecimal highVolumeThreshold = new BigDecimal(1.25);
        Iterator pricesIt = prices.iterator();
        Iterator volumeChangesIt = volumeChanges.iterator();

        List<List<BigDecimal>> band = getBollingerBand(bband, bband.length);

        BigDecimal lower = band.get(0).get(0);
        BigDecimal mid = band.get(1).get(0);
        BigDecimal upper = band.get(2).get(0);

        Boolean shortTarget = (lastPrice.compareTo(upper) <= 0) && (lastPrice.compareTo(mid) >= 0);
        Boolean longTarget = (lastPrice.compareTo(mid) <= 0) && (lastPrice.compareTo(lower) >= 0);
        Boolean bbShort = false;
        Boolean bbLong = false;
        Boolean bbBroken = false;

        if (volumeChanges.size() == prices.size()) {
            while (pricesIt.hasNext() && volumeChangesIt.hasNext()) {
                BigDecimal volumeRatio = (BigDecimal) volumeChangesIt.next();
                BigDecimal price = (BigDecimal) pricesIt.next();

                if (volumeRatio.compareTo(lowVolumeThreshold) < 0) {
                    lowVolumeDays++;
                } else if (volumeRatio.compareTo(highVolumeThreshold) > 0) {
                    highVolumeDays++;
                }

                if (previousPrice != null) {
                    if (price.compareTo(previousPrice) < 0) {
                        downward++;
                    } else if (price.compareTo(previousPrice) > 0) {
                        upward++;
                    }
                }

                if (!bbBroken) {
                    if (shortTarget) {

                        if(price.compareTo(upper) > 0) {
                            bbShort = true;
                            bbBroken = true;
                        }
                    } else if (longTarget) {

                        if(price.compareTo(lower) < 0) {
                            bbLong = true;
                            bbBroken = true;
                        }
                    }
                }

                previousPrice = price;
            }
        }

        if (lowVolumeDays > highVolumeDays && lowVolumeDays > 2) {

            if (downward > upward) {
                recommendation = Action.SELL;
            } else if (downward < upward) {
                recommendation = Action.BUY;
            }
        } else if (lowVolumeDays < highVolumeDays && highVolumeDays > 2) {

            if (downward > upward) {
                recommendation = Action.SELL;
            } else if (downward < upward) {
                recommendation = Action.BUY;
            }
        }

        if (bbShort) {
            if (recommendation == Action.SELL) {
                recommendation = Action.STRONGSELL;
            } else if (recommendation == Action.BUY) {
                recommendation = Action.INDETERMINANT;
            } else {
                recommendation = Action.INDETERMINANT;
            }
        } else if (bbLong) {
            if (recommendation == Action.BUY) {
                recommendation = Action.STRONGBUY;
            } else if (recommendation == Action.SELL) {
                recommendation = Action.INDETERMINANT;
            } else {
                recommendation = Action.INDETERMINANT;
            }
        }

        return recommendation;
    }

    // BUY on Friday. SELL on Monday.
    private Action testDecisionAlgo(Quote quote, BigDecimal avg30, BigDecimal avg90,
                                             Deque<BigDecimal> volumeChanges, Deque<BigDecimal> prices) {
        Action recommendation = Action.INDETERMINANT;
        if (DateParser.getDayOfWeek(quote.getDate()) == Calendar.FRIDAY) {
            recommendation = Action.BUY;
        } else if (DateParser.getDayOfWeek(quote.getDate()) == Calendar.MONDAY) {
            recommendation = Action.SELL;
        }

        return recommendation;
    }

    private Deque<BigDecimal> addToQueue(BigDecimal value, Deque<BigDecimal> queue, int maxSize) throws Exception{
        if (value == null) {
            log.error("Found null value");
            return queue;
        }

        if (queue.size() < maxSize) {
            queue.push(value);
        } else {
            queue.removeLast();
            queue.push(value);
        }
        return queue;
    }

    private BacktestSummary calculateReturns(List<Signal> signals, BigDecimal deviation) {
        BacktestSummary results = new BacktestSummary();
        log.info("signals: {}", signals.size());

        for (Signal signal : signals) {
            // if (Statistics.percentDifference(signal.getShortTermAverage(), signal.getLongTermAverage()).abs().compareTo(deviation) <= 0) {
                if (signal.getAction() == Action.STRONGSELL
                      //  || signal.getAction() == Action.SELL
                        ) {
                    if (results.buys.size() > 0) {
                        results.totalTrades++;
                        // log.info("Holdings: {}", results.buys.toString());

                        BigDecimal holding = results.buys.removeFirst();
                        BigDecimal profit = signal.getClose().subtract(holding);
                        results.invested = results.invested.add(holding);
                        results.total = results.total.add(profit);
                        // log.info("SELL on {} cost: {} 1@{} Profit: {}", signal.getDate(), holding, signal.getClose(), profit);
                    }
                } else if (signal.getAction() == Action.STRONGBUY
                     //   || signal.getAction() == Action.BUY
                        ) {
                    results.totalTrades++;
                    results.buys.add(signal.getClose());
                    // log.info("BUY {} 1@{}", signal.getDate(), signal.getClose());

                }
            // }
        }

        if (results.total.compareTo(BigDecimal.ZERO) != 0 && results.invested.compareTo(BigDecimal.ZERO) != 0) {
            results.returns = results.total.divide(results.invested, 4, RoundingMode.HALF_EVEN);
        }

        return results;
    }

    private List<List<BigDecimal>> getBollingerBand(BigDecimal[] real, int period) {
        BBandOptions body = new BBandOptions(real, period, 2);
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<BBandOptions> entity = new HttpEntity<>(body);

        ResponseEntity<List<List<BigDecimal>>> rateResponse =
                restTemplate.exchange("http://localhost:9000/api/backtest/bbands",
                        HttpMethod.POST, entity, new ParameterizedTypeReference<List<List<BigDecimal>>>() {});

        List<List<BigDecimal>> bbands = rateResponse.getBody();

        return bbands;
    }

    public class BacktestSummary {
        public int totalTrades = 0;
        public BigDecimal total = BigDecimal.ZERO;
        public BigDecimal invested = BigDecimal.ZERO;
        public BigDecimal returns = BigDecimal.ZERO;
        public long lastVolume;
        public BigDecimal lastPrice;
        public Action recommendation;
        Deque<BigDecimal> buys = new ArrayDeque<>();
    }

}
