package com.robinhoodanalytics.backtestservice.backtest;

import com.robinhoodanalytics.backtestservice.models.Order;
import com.robinhoodanalytics.backtestservice.models.StockRank;
import com.robinhoodanalytics.backtestservice.models.TradingContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BacktestContext implements TradingContext{
    private List<StockRank> _stocks;
    private List<Order> _orders = new ArrayList<>();
    private List<BigDecimal> _accountHistory = new ArrayList<>();
    private BigDecimal _initialFund;
    private BigDecimal _cash;
    private BigDecimal _netValue;

    BacktestContext(List<StockRank> stocks, BigDecimal initialFund) {
                        _stocks = stocks;
                        _initialFund = initialFund;
                        _cash = initialFund;
                        _netValue = initialFund; 
                    }
    @Override
    public List<StockRank> getStocks() {
        return _stocks;
    }

    @Override
    public BigDecimal getInitialFund() {
        return _initialFund;
    }

    @Override
    public BigDecimal getCash() {
        return _cash;
    }
}
