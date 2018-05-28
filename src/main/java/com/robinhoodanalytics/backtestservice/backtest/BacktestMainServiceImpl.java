package com.robinhoodanalytics.backtestservice.backtest;

import com.robinhoodanalytics.backtestservice.models.Quote;
import com.robinhoodanalytics.backtestservice.models.Signal;
import com.robinhoodanalytics.backtestservice.models.StockRank;
import com.robinhoodanalytics.backtestservice.quotes.QuoteService;
import com.robinhoodanalytics.backtestservice.strategy.BuyAndHold;
import com.robinhoodanalytics.backtestservice.utils.RollingAverage;
import com.robinhoodanalytics.backtestservice.utils.Statistics;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component("backtestMainService")
public class BacktestMainServiceImpl
    implements BacktestMainService
{
    QuoteService _quoteService;

    @Override
    public List<Signal> executeMeanReversion(String symbol, Date from, Date to, BigDecimal deviation, int shortTerm, int longTerm)
    {
        return new ArrayList<>();
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

    public List<Signal> trainMeanReversion(String symbol,
                                                           Date from,
                                                           Date to,
                                                           int shortTermWindow,
                                                           int longTermWindow
    )
    {
        List<Quote> quotes = _quoteService.getHistoricalQuotes(symbol, from, to);

        RollingAverage shortTerm = new RollingAverage(shortTermWindow);
        RollingAverage longTerm = new RollingAverage(longTermWindow);
        RollingAverage volumeWindow = new RollingAverage(longTermWindow);
        List<Signal> results = new ArrayList<>();

        int preload = 0;

        for (Quote quote : quotes) {
            shortTerm.add(quote.getClose());
            longTerm.add(quote.getClose());
            if (preload < longTermWindow) {
                preload++;
            } else {
                BigDecimal shortAvg = shortTerm.getAverage();
                BigDecimal longAvg = longTerm.getAverage();
                BigDecimal pctChange = Statistics.percentDifference(shortAvg, longAvg).abs();
                BigDecimal volumeChange = Statistics.percentChange(volumeWindow.getAverage(), new BigDecimal(quote.getVolume()));
                Signal sig = new Signal(quote.getDate(), null,
                        pctChange, shortAvg, longAvg, quote.getClose());
            }
        }
        return results;
    }
}
