package com.robinhoodanalytics.backtestservice.strategy;

import com.robinhoodanalytics.backtestservice.models.Quote;
import com.robinhoodanalytics.backtestservice.models.TradingContext;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface TradingStrategy {
    public void initialize(TradingContext context);
    public void onTick(Date date, Map<String, Quote> today);
    public void rebalance();
}
