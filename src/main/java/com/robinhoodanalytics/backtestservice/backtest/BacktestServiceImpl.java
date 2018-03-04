package com.robinhoodanalytics.backtestservice.backtest;

import com.robinhoodanalytics.backtestservice.models.Signal;
import com.robinhoodanalytics.backtestservice.models.StockRank;
import com.robinhoodanalytics.backtestservice.quotes.QuoteService;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class BacktestServiceImpl
    implements BacktestService
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
        for(StockRank stock: stocks) {
            _quoteService.getHistoricalQuotes(stock.getSymbol(), from, to);
        }        
        return new ArrayList<>();
    }

    public List<Signal> trainMeanReversion(String symbol,
                                                           Date from,
                                                           Date to,
                                                           int shortTermStart,
                                                           int longTermStart,
                                                           int shortTermEnd,
                                                           int longTermEnd
    )
    {
        return new ArrayList<>();
    }
}
