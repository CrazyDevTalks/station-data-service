package com.robinhoodanalytics.backtestservice.backtest.models;

import java.util.Date;

public class BacktestSettings {
    public String symbol;
    public String strategy;
    public Date from;
    public Date to;
    public double[] settings;
}
