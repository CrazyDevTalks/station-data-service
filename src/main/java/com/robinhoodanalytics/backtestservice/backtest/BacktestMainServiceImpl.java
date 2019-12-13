package com.robinhoodanalytics.backtestservice.backtest;

import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.backtest.models.PrecogBacktestResults;
import com.robinhoodanalytics.backtestservice.models.*;
import com.robinhoodanalytics.backtestservice.precog.PrecogService;
import com.robinhoodanalytics.backtestservice.quotes.QuoteService;
import com.robinhoodanalytics.backtestservice.strategy.*;
import com.robinhoodanalytics.backtestservice.trainer.TrainerService;
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
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Component("backtestMainService")
public class BacktestMainServiceImpl
    implements BacktestMainService {

    @Autowired
    QuoteService _quoteService;

    @Autowired
    TrainerService _trainerService;

    @Autowired
    PrecogService _precogService;

    private static final Logger log = LoggerFactory.getLogger(BacktestServiceApplication.class);

    private static boolean logOn = false;

    @Override
    public PrecogBacktestResults[] backtestRnn(String symbol, Date from, Date to) {
        AggregatedQuote[] baseline = _trainerService.convertTrainingData(symbol, from, to);
//        List<Quote> uproQuotes = _trainerService.sanitizeQuotes(symbol, from, to);
//        List<Quote> spxuQuotes = _trainerService.sanitizeQuotes(symbol, from, to);

        ResponseEntity<int[]> test = _precogService.retrievePrediction(baseline[0].input);
//        for (int i = 0; i < baseline.length; i++) {
//            baseline[i].input
//        }

        PrecogBacktestResults[] results = new PrecogBacktestResults[1];
        results[0] = new PrecogBacktestResults(symbol, baseline[0].input, test.getBody()[0], 0, null);
        return results;
    }

    @Override
    public List<Signal> getMeanReversionTimeline(String symbol, Date from, Date to, int shortTerm, int longTerm)
            throws Exception {
        return trainMeanReversion(symbol, from, to, shortTerm, longTerm);
    }

    @Override
    public BacktestSummary getMeanReversionResults(String symbol, Date from, Date to, BigDecimal deviation, int shortTerm, int longTerm)
            throws Exception {
        List<Signal> signals = trainMeanReversion(symbol, from, to, shortTerm, longTerm);
        BacktestSummary summary = calculateReturns(signals);

        if (signals.size() > 0) {
            Signal lastSignal = signals.get(signals.size() - 1);
            summary.lastPrice = lastSignal.getClose();
            summary.lastVolume = lastSignal.getVolume();
            summary.recommendation = lastSignal.getAction();
            summary.algo = "BBands";
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
                                            int longTermWindow) throws Exception {
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
                BigDecimal volumeChange = Statistics.percentChange(volumeWindow.getAverage(),
                        new BigDecimal(quote.getVolume()));

                recentVolumeChanges = addToQueue(volumeChange, recentVolumeChanges, 10);

                recentPrices = addToQueue(quote.getClose(), recentPrices, 10);

                Action action = getDecision(quote, recentVolumeChanges,
                        recentPrices, bars);

                Signal sig = new Signal(quote.getDate(), action, pctChange,
                        shortAvg, longAvg, volumeChange,
                        quote.getClose(), quote.getVolume());
                results.add(sig);
            }
        }
        return results;
    }

    private Action getDecision(Quote quote, Deque<BigDecimal> volumeChanges,
                               Deque<BigDecimal> prices, List<Bar> bars) {
        return getMeanReversionDirection(quote, volumeChanges, prices, bars);
    }

    private Action getMeanReversionDirection(Quote quote, Deque<BigDecimal> volumeChanges, Deque<BigDecimal> prices, List<Bar> bars) {
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

        if (lowVolumeDays > highVolumeDays && lowVolumeDays > 3) {

            if (downward > upward) {
                recommendation = Action.SELL;
            } else if (upward > downward) {
                recommendation = Action.BUY;
            }
        } else if (highVolumeDays > lowVolumeDays && highVolumeDays > 3) {

            if (downward > upward) {
                recommendation = Action.BUY;
            } else if (upward > downward) {
                recommendation = Action.SELL;
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
            if ((lastPrice.compareTo(upper) > 0)) {
                recommendation = Action.SELL;
            } else if (lastPrice.compareTo(lower) < 0) {
                recommendation = Action.BUY;
            } else  {
                recommendation = Action.INDETERMINANT;
            }
        }

        return recommendation;
    }

    private Deque<BigDecimal> addToQueue(BigDecimal value, Deque<BigDecimal> queue, int maxSize) {
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

    private BacktestSummary calculateReturns(List<Signal> signals) {
        Stock s = new Stock("");
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
                    Order o = new Order(s, 1, signal.getClose(), Order.Side.sell, signal.getDate());
                    results.orderHistory.add(o);
                } else {
                    printLog("Shorting on: ", signal.getDate(), results.buys);

                    results.totalTrades++;
                    results.buys.add(signal.getClose().multiply(new BigDecimal(-1)));
                    Order o = new Order(s, 1, signal.getClose().multiply(new BigDecimal(-1)), Order.Side.sell, signal.getDate());
                    results.orderHistory.add(o);
                }
            } else if (signal.getAction() == Action.STRONGBUY) {
                results.totalTrades++;
                if (results.buys.size() > 0 && results.buys.peekLast().compareTo(BigDecimal.ZERO) < 0) {
                    BigDecimal holding = results.buys.removeLast();
                    BigDecimal profit = holding.add(signal.getClose()).multiply(new BigDecimal(-1));

                    results.invested = results.invested.add(holding.multiply(new BigDecimal(-1)));
                    results.total = results.total.add(profit);

                    Order o = new Order(s, 1, signal.getClose(), Order.Side.buy, signal.getDate());
                    results.orderHistory.add(o);
                } else {
                    results.buys.add(signal.getClose());
                    Order o = new Order(s, 1, signal.getClose(), Order.Side.buy, signal.getDate());
                    results.orderHistory.add(o);
                }
            }
        }

        if (results.total.compareTo(BigDecimal.ZERO) != 0 && results.invested.compareTo(BigDecimal.ZERO) != 0) {
            results.returns = results.total.divide(results.invested, 4, RoundingMode.HALF_EVEN);
        }

        return results;
    }

    private TimeSeries convertToTimeSeries(String symbol, List<Bar> bars) {
        return new BaseTimeSeries(symbol + "bars", bars);
    }

    private BaseBar createBar(String symbol, ZonedDateTime date, double open, double high,
                              double low, double close, double volume) {

        return new BaseBar(date, open, high, low, close, volume);
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

    @Override
    public BacktestSummary backtestStrategy(String symbol, String strategy, Date from, Date to, double[] settings)
        throws Exception {
        if (strategy == "BollingerBand") {
            return getMeanReversionResults(symbol, from, to, new BigDecimal(settings[0], MathContext.DECIMAL64), (int) settings[1], (int) settings[2]);
        }

        List<Quote> quotes = _quoteService.getHistoricalQuotes(symbol, from, to);

        switch(strategy.toUpperCase()) {
            case "MONEYFLOWINDEX":
                return runMoneyFlowIndexBacktest(quotes);
            case "BBMFI":
                return runBbandMoneyFlowIndexBacktest(quotes);
            case "MOVINGAVERAGECROSSOVER":
                return runMovingAverageCrossOver(quotes, new BigDecimal(settings[0], MathContext.DECIMAL64),
                        (int) settings[1], (int) settings[2]);
            case "FINDRESISTANCE":
                return runFindResistance(quotes);
        }

        return null;
    }

    @Override
    public ResponseEntity trainSignals(String symbol, String strategy, Date from, Date to) {
        List<Quote> quotes = _quoteService.getHistoricalQuotes(symbol, from, to);
        List<Signal> signals = getBbandMfiSignals(quotes);
        return null;
    }

    private BacktestSummary runBbandMoneyFlowIndexBacktest(List<Quote> quotes) {
        List<Signal> signals = this.getBbandMfiSignals(quotes);
        BacktestSummary summary = calculateReturns(signals);

        if (signals.size() > 0) {
            Signal lastSignal = signals.get(signals.size() - 1);
            summary.lastPrice = lastSignal.getClose();
            summary.lastVolume = lastSignal.getVolume();
            summary.recommendation = lastSignal.getAction();
            summary.algo = "MoneyFlowIndex";
            summary.signals = signals;
        }

        return summary;
    }

    private List<Signal> getBbandMfiSignals(List<Quote> quotes) {
        BbandMfi bb = new BbandMfi();
        int period = 14;
        int bbandPeriod = 80;
        List<Signal> signals = new ArrayList<>();
        log.info("Quotes: {}", quotes.size());
        for (int i = 0; i < quotes.size(); i++) {
            if (i > bbandPeriod) {
                List<Quote> sublist = quotes.subList(i - 14, i + 1);
                MfiPayload payload = this.createList(sublist, period);
                MfiPayload bbList = this.createList(quotes.subList(i - bbandPeriod, i + 1), bbandPeriod);

                bb.high = payload.high;
                bb.low = payload.low;
                bb.close = payload.close;
                bb.volume = payload.volume;
                bb.mfiPeriod = payload.period;
                bb.bbandPeriod = bbandPeriod;
                bb.bbandClose = bbList.close;
                Signal technicalSignal = bb.onTick(sublist.get(sublist.size() - 1).getDate());

                technicalSignal = this.setOneMonthGain(quotes, i + 20, technicalSignal);
                technicalSignal = this.setOneWeekGain(quotes, i + 5, technicalSignal);
                signals.add(technicalSignal);
            }
        }

        return signals;
    }

    private Signal setOneMonthGain(List<Quote> quotes, int oneMonthIdx, Signal currentSignal) {
        if (oneMonthIdx < quotes.size()) {
            BigDecimal futureClose = quotes.get(oneMonthIdx).getClose();
            BigDecimal currentClose = currentSignal.getClose();
            BigDecimal diff = futureClose.subtract(currentClose);
            diff = diff.divide(currentClose, RoundingMode.HALF_UP);

            if (currentSignal.getAction() == Action.SELL || currentSignal.getAction() == Action.STRONGSELL) {
                diff = diff.multiply(new BigDecimal(-1));
            }

            currentSignal.oneMonthGain = diff;
        }
        return currentSignal;
    }

    private Signal setOneWeekGain(List<Quote> quotes, int oneWeekIdx, Signal currentSignal) {
        if (oneWeekIdx < quotes.size()) {
            BigDecimal futureClose = quotes.get(oneWeekIdx).getClose();
            BigDecimal currentClose = currentSignal.getClose();
            BigDecimal diff = futureClose.subtract(currentClose);
            diff = diff.divide(currentClose, RoundingMode.HALF_UP);

            if (currentSignal.getAction() == Action.SELL || currentSignal.getAction() == Action.STRONGSELL) {
                diff = diff.multiply(new BigDecimal(-1));
            }

            currentSignal.oneWeekGain = diff;
        }
        return currentSignal;
    }


    private BacktestSummary runMoneyFlowIndexBacktest(List<Quote> quotes) {
        List<Signal> signals = this.getMfiSignals(quotes);
        BacktestSummary summary = calculateReturns(signals);

        if (signals.size() > 0) {
            Signal lastSignal = signals.get(signals.size() - 1);
            summary.lastPrice = lastSignal.getClose();
            summary.lastVolume = lastSignal.getVolume();
            summary.recommendation = lastSignal.getAction();
            summary.algo = "MoneyFlowIndex";
            summary.signals = signals;
        }

        return summary;
    }

    private List<Signal> getMfiSignals(List<Quote> quotes) {
        MoneyFlowIndex mfi = new MoneyFlowIndex();
        int period = 14;
        List<Signal> signals = new ArrayList<>();

        for (int i = 0; i< quotes.size(); i++) {
            if (i > 14) {
                List<Quote> sublist = quotes.subList(i - 14, i + 1);
                MfiPayload payload = this.createList(sublist, period);
                mfi.high = payload.high;
                mfi.low = payload.low;
                mfi.close = payload.close;
                mfi.volume = payload.volume;
                mfi.period = payload.period;
                Signal technicalSignal = mfi.onTick(sublist.get(sublist.size() - 1).getDate());

                if (technicalSignal != null) {
                    signals.add(technicalSignal);
                    int oneMonthIdx = i + 20;
                    if (oneMonthIdx < quotes.size()) {
                        BigDecimal latterClose = new BigDecimal(String.valueOf(quotes.get(oneMonthIdx).getClose()));
                        BigDecimal diff = latterClose.subtract(technicalSignal.getClose());
                        diff = diff.divide(technicalSignal.getClose(), RoundingMode.HALF_UP);

                        if (technicalSignal.getAction() == Action.SELL || technicalSignal.getAction() == Action.STRONGSELL) {
                            diff = diff.multiply(new BigDecimal(-1));
                        }

                        if (technicalSignal.oneMonthGain != null) {
                            technicalSignal.oneMonthGain.add(diff);
                        } else {
                            technicalSignal.oneMonthGain = diff;
                        }
                    }
                }
            }
        }

        return signals;
    }

    private MfiPayload createList(List<Quote> quotes, int period) {
        double[] high = new double[period + 1];
        double[] low = new double[period + 1];
        double[] close = new double[period + 1];
        long[] volume = new long[period + 1];

        for (int i = 0; i< quotes.size(); i++) {
            high[i] = quotes.get(i).getHigh().doubleValue();
            low[i] = quotes.get(i).getLow().doubleValue();
            close[i] = quotes.get(i).getClose().doubleValue();
            volume[i] = quotes.get(i).getVolume();
        }

        return new MfiPayload(high, low, close, volume, period);
    }

    private List<Signal> getMACrossoverSignals(List<Quote> quotes,
                                               BigDecimal deviation,
                                               int shortTermSize,
                                               int longTermSize) {
        MovingAverageCrossover ma = new MovingAverageCrossover();

        ma.shortTermWindow = new RollingAverage(shortTermSize);
        ma.longTermWindow = new RollingAverage(longTermSize);

        List<Signal> signals = new ArrayList<>();
        Deque<BigDecimal> shortTermAvg = new ArrayDeque<>();

        for (int i = 0; i < quotes.size(); i++) {
            Quote quote = quotes.get(i);
            if (quote.getClose() == null) {
                log.error("Closing price missing: {}", quote.toString());
            } else {
                ma.shortTermWindow.add(quote.getClose());
                ma.longTermWindow.add(quote.getClose());
                ma.shortTermAverageHistory = shortTermAvg;

                Signal technicalSignal = ma.onTick(quote.getDate());

                technicalSignal.shortTermSize = shortTermSize;
                technicalSignal.longTermSize = longTermSize;

                signals.add(addSignalData(i, quote, quotes, technicalSignal));

                shortTermAvg = addToQueue(ma.shortTermWindow.getAverage(), shortTermAvg, 5);
            }
        }

        return signals;
    }

    private Signal addSignalData(int index, Quote quote, List<Quote> quotes, Signal signal) {
        signal.setClose(quote.getClose());
        signal.setVolume(quote.getVolume());
        signal = setOneMonthGain(quotes, index + 20, signal);
        signal = setOneWeekGain(quotes, index + 5, signal);
        return signal;
    }

    private BacktestSummary runMovingAverageCrossOver(List<Quote> quotes, BigDecimal deviation, int shortTerm, int longTerm) {
        List<Signal> signals = getMACrossoverSignals(quotes, deviation, shortTerm, longTerm);
        BacktestSummary summary = calculateReturns(signals);

        if (signals.size() > 0) {
            Signal lastSignal = signals.get(signals.size() - 1);
            summary.lastPrice = lastSignal.getClose();
            summary.lastVolume = lastSignal.getVolume();
            summary.recommendation = lastSignal.getAction();
            summary.algo = "MovingAverageCrossOver";
            summary.signals = signals;
        }

        return summary;
    }

    private BacktestSummary runFindResistance(List<Quote> quotes) {
        List<Signal> signals = this.getResistanceSignals(quotes);
        BacktestSummary summary = calculateReturns(signals);

        if (signals.size() > 0) {
            Signal lastSignal = signals.get(signals.size() - 1);
            summary.lastPrice = lastSignal.getClose();
            summary.lastVolume = lastSignal.getVolume();
            summary.recommendation = lastSignal.getAction();
            summary.algo = "FindResistance";
            summary.signals = signals;
        }

        return summary;
    }

    private List<Signal> getResistanceSignals(List<Quote> quotes) {
        List<Signal> signals = new ArrayList<>();

        int windowSize = 10;
        FindResistance eq = new FindResistance(quotes, windowSize,
                4, 300);

        for (int i = 0; i < quotes.size(); i++) {
            Quote quote = quotes.get(i);
            eq.setParameters(quote, i);

            Signal technicalSignal = eq.onTick(quote.getDate());

            signals.add(this.addSignalData(i, quote, quotes, technicalSignal));
        }
        return signals;
    }



    private void printLog(String title, Date d, Object o) {
        if(logOn) {
            log.info("Log{ {} {} {}", d, title, o, "}");
        }
    }
}
