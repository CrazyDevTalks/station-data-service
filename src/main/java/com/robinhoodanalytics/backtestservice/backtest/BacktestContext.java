package com.robinhoodanalytics.backtestservice.backtest;

import com.robinhoodanalytics.backtestservice.models.Order;
import com.robinhoodanalytics.backtestservice.models.StockRank;
import com.robinhoodanalytics.backtestservice.models.TradingContext;

import java.math.BigDecimal;
import java.util.List;

public class BacktestContext implements TradingContext{
    private List<StockRank> stocks;
    private List<Order> orders;
    private BigDecimal[] accountHistory;
    private BigDecimal initialFund;
    private BigDecimal cash;
    private BigDecimal netValue;

    @Override
    public List<StockRank> getStocks() {
        return stocks;
    }

    @Override
    public BigDecimal getInitialFund() {
        return initialFund;
    }

    @Override
    public BigDecimal getCash() {
        return cash;
    }
}
