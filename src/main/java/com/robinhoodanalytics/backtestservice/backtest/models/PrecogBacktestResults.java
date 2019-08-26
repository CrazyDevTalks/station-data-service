package com.robinhoodanalytics.backtestservice.backtest.models;

import java.time.LocalDateTime;

public class PrecogBacktestResults {
    public String symbol;
    public double[] previousDayInput;
    public double predicted;
    public double actual;
    public LocalDateTime date;

    public PrecogBacktestResults(String symbol, double[] input, double predicted, double actual, LocalDateTime date) {
        this.symbol = symbol;
        this.previousDayInput = input;
        this.predicted = predicted;
        this.actual = actual;
        this.date = date;
    }
}
