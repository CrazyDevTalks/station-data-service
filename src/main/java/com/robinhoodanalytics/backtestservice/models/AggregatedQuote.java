package com.robinhoodanalytics.backtestservice.models;

import java.util.Date;

public class AggregatedQuote {
    public double[] _input;
    public Date _date;

    public AggregatedQuote(Date date, double[] input) {
        this._date = date;
        this._input = input;
    }
}
