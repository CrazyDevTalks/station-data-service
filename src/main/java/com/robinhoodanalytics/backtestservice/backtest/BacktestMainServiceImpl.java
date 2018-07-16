package com.robinhoodanalytics.backtestservice.backtest;

import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.models.*;
import com.robinhoodanalytics.backtestservice.quotes.QuoteService;
import com.robinhoodanalytics.backtestservice.strategy.BuyAndHold;
import com.robinhoodanalytics.backtestservice.utils.DateParser;
import com.robinhoodanalytics.backtestservice.utils.RollingAverage;
import com.robinhoodanalytics.backtestservice.utils.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

@Component("backtestMainService")
public class BacktestMainServiceImpl
    implements BacktestMainService {
    @Autowired
    QuoteService _quoteService;

    private static final Logger log = LoggerFactory.getLogger(BacktestServiceApplication.class);

    private static boolean logOn = false;

    @Override
    public List<Signal> getMeanReversionTimeline(String symbol, Date from, Date to, int shortTerm, int longTerm, int bbandPeriod)
            throws Exception {
        return trainMeanReversion(symbol, from, to, shortTerm, longTerm, bbandPeriod);
    }

    @Override
    public BacktestSummary getMeanReversionResults(String symbol, Date from, Date to, BigDecimal deviation, int shortTerm, int longTerm, int bbandPeriod)
            throws Exception {
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
    public List<Signal> buyAndHold(List<String> symbols, Date from, Date to, BigDecimal initialFund) {
        List<StockRank> stocks = new ArrayList<>();
        for (String symbol : symbols) {
            stocks.add(new StockRank(symbol, 1.0));
        }
        BacktestContext context = new BacktestContext(stocks, initialFund);
        BuyAndHold buyHold = new BuyAndHold();

        buyHold.initialize(context);

        Map<String, List<Quote>> data = new HashMap<>();

        for (StockRank stock : stocks) {
            _quoteService.getHistoricalQuotes(stock.getSymbol(), from, to);
        }

        return new ArrayList<>();
    }

    private List<Signal> trainMeanReversion(String symbol,
                                            Date from,
                                            Date to,
                                            int shortTermWindow,
                                            int longTermWindow,
                                            int bbandPeriod) throws Exception {
        List<Quote> quotes = _quoteService.getHistoricalQuotes(symbol, from, to);

        RollingAverage shortTerm = new RollingAverage(shortTermWindow);
        RollingAverage longTerm = new RollingAverage(longTermWindow);
        List<Bar> bars = new ArrayList<>();
        RollingAverage volumeWindow = new RollingAverage(longTermWindow);
        Deque<BigDecimal> recentVolumeChanges = new ArrayDeque<>();
        Deque<BigDecimal> recentPrices = new ArrayDeque<>();

        List<Signal> results = new ArrayList<>();

        for (Quote quote : quotes) {
            if (quote.getClose() == null) {
                log.error("Closing price missing: {}", quote.toString());
            } else {
                shortTerm.add(quote.getClose());
                longTerm.add(quote.getClose());

                ZonedDateTime d = ZonedDateTime.ofInstant(quote.getDate().toInstant(),
                        ZoneId.of("America/New_York"));

                bars.add(createBar(quote.getSymbol(), d, quote.getOpen().doubleValue(),
                        quote.getHigh().doubleValue(), quote.getLow().doubleValue(),
                        quote.getClose().doubleValue(), quote.getVolume()));

                volumeWindow.add(new BigDecimal(quote.getVolume()));

                BigDecimal shortAvg = BigDecimal.ONE;
                BigDecimal longAvg = BigDecimal.ONE;

                BigDecimal pctChange = BigDecimal.ONE;
                BigDecimal volumeChange = Statistics.percentChange(volumeWindow.getAverage(), new BigDecimal(quote.getVolume()));

                recentVolumeChanges = addToQueue(volumeChange, recentVolumeChanges, 10);

                recentPrices = addToQueue(quote.getClose(), recentPrices, 10);

                Action action = getDecision(quote, shortAvg, longAvg, recentVolumeChanges, recentPrices, bars);

                Signal sig = new Signal(quote.getDate(), action,
                        pctChange, shortAvg, longAvg, volumeChange, quote.getClose(), quote.getVolume());
                results.add(sig);
            }
        }
        return results;
    }

    private Action getDecision(Quote quote, BigDecimal avg30, BigDecimal avg90,
                               Deque<BigDecimal> volumeChanges, Deque<BigDecimal> prices, List<Bar> bars) {
        return getMeanReversionDirection(quote, avg30, avg90,
                volumeChanges, prices, bars);
    }

    private Action getMeanReversionDirection(Quote quote, BigDecimal avg30, BigDecimal avg90,
                                             Deque<BigDecimal> volumeChanges, Deque<BigDecimal> prices, List<Bar> bars) {
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

        TimeSeries series = convertToTimeSeries(quote.getSymbol(), bars);

        BollingerBand bband = getBollingerBandV2(series);
        BigDecimal lower = bband.lower;
        BigDecimal mid = bband.mid;
        BigDecimal upper = bband.upper;

        printLog("bband: ", quote.getDate(), bband.toString());

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

                        if (price.compareTo(upper) > 0) {
                            bbShort = true;
                            bbBroken = true;
                        }
                    } else if (longTarget) {

                        if (price.compareTo(lower) < 0) {
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

        printLog("recommendation: ", null, recommendation);

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
        } else {
            recommendation = Action.INDETERMINANT;
        }
        printLog("short: ", null, bbShort);
        printLog("Long: ", null, bbLong);

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

    private Deque<BigDecimal> addToQueue(BigDecimal value, Deque<BigDecimal> queue, int maxSize) throws Exception {
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
            if (signal.getAction() == Action.STRONGSELL) {
                if (results.buys.size() > 0) {
                    printLog("Selling on: ", signal.getDate(), results.buys);

                    results.totalTrades++;
                    BigDecimal holding = results.buys.removeFirst();
                    BigDecimal profit = signal.getClose().subtract(holding);
                    results.invested = results.invested.add(holding);
                    results.total = results.total.add(profit);

                    printLog("Selling:", null, holding);
                    printLog("@ ", null, signal.getClose());

                }
            } else if (signal.getAction() == Action.STRONGBUY) {
                results.totalTrades++;
                results.buys.add(signal.getClose());
            }
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
                        HttpMethod.POST, entity, new ParameterizedTypeReference<List<List<BigDecimal>>>() {
                        });

        List<List<BigDecimal>> bbands = rateResponse.getBody();

        return bbands;
    }

    private TimeSeries convertToTimeSeries(String symbol, List<Bar> bars) {
        return new BaseTimeSeries(symbol + "bars", bars);
    }

    private BaseBar createBar(String symbol, ZonedDateTime date, double open, double high,
                              double low, double close, double volume) {

        return new BaseBar(date, open, high, low, close, volume);
    }

    private BollingerBand getBollingerBandV1(BigDecimal[] bband) {
        List<List<BigDecimal>> band = getBollingerBand(bband, bband.length);

        BigDecimal lower = band.get(0).get(0);
        BigDecimal mid = band.get(1).get(0);
        BigDecimal upper = band.get(2).get(0);
        return new BollingerBand(lower, mid, upper);
    }

    private BollingerBand getBollingerBandV2(TimeSeries series) {
        BigDecimal k = new BigDecimal(2);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator avg80 = new SMAIndicator(closePrice, 80);
        StandardDeviationIndicator sd80 = new StandardDeviationIndicator(closePrice, 80);

        BollingerBandsMiddleIndicator middleBBand = new BollingerBandsMiddleIndicator(avg80);

        int endIndx = middleBBand.getTimeSeries().getEndIndex();

        BigDecimal mid = middleBBand.getIndicator().getValue(endIndx).getDelegate();
        BigDecimal lower = mid.subtract(sd80.getValue(endIndx).getDelegate().multiply(k));
        BigDecimal upper = mid.add(sd80.getValue(endIndx).getDelegate().multiply(k));

        return new BollingerBand(lower, mid, upper);
    }

    private void printLog(String title, Date d, Object o) {
        if(logOn) {
            log.info("Log{ {} {} {}", d, title, o, "}");
        }
    }
}
