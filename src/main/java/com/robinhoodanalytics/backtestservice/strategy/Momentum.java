package com.robinhoodanalytics.backtestservice.strategy;

import com.robinhoodanalytics.backtestservice.models.Order;
import com.robinhoodanalytics.backtestservice.models.Quote;
import com.robinhoodanalytics.backtestservice.models.TradingContext;
import com.robinhoodanalytics.backtestservice.utils.RollingAverage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Momentum implements TradingStrategy {
    private TradingContext context;
    private RollingAverage movingAverage;
    private int period = 50;

    @Override
    public void initialize(TradingContext context) {
        this.context = context;
        movingAverage = new RollingAverage(period);
    }

    public void beforeTradingDay() {

    }

    @Override
    public void onTick(Date date, Map<String, Quote> today) {
//        List<Order> orders = new ArrayList<>();
//
//        movingAverage.add(quote.getClose());
//        if (movingAverage.getSamples().length > period) {
//            if (quote.getClose().compareTo(movingAverage.getAverage()) > 0) {
//                Order newOrder = new Order();
//                orders.add(newOrder);
//            }
//        }

    }

    @Override
    public void rebalance() {

    }
}
