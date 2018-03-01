package com.robinhoodanalytics.backtestservice.strategy;

import com.robinhoodanalytics.backtestservice.models.Quote;
import com.robinhoodanalytics.backtestservice.models.TradingContext;

public interface TradingStrategy {
    public void initialize(TradingContext context);
    public void beforeTradingDay();
    public void onTick(Quote quote);
    public void rebalance();
}
