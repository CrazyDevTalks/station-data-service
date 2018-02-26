package com.robinhoodanalytics.backtestservice.backtest;

import com.robinhoodanalytics.backtestservice.models.Order;
import com.robinhoodanalytics.backtestservice.models.StockRank;
import com.robinhoodanalytics.backtestservice.models.TradingContext;

import java.util.List;

public class BacktestContext implements TradingContext{
    List<StockRank> stocks;
    List<Order> orders;
    double[] accountHistory;
    double initialFund;
    double netValue;
}
