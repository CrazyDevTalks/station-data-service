package com.robinhoodanalytics.backtestservice.models;

import java.util.Date;

public class AggregatedQuote {
    public double[] _input;
    public Date _date;
    public double[] _output;

    public AggregatedQuote(Date date, double[] input, double[] output) {
        this._date = date;
        this._input = input;
        this._output = output;
    }

    public void set_output(double[] _output) {
        this._output = _output;
    }

    public void set_date(Date _date) {
        this._date = _date;
    }

    public void set_input(double[] _input) {
        this._input = _input;
    }

    public Date get_date() {
        return _date;
    }

    public double[] get_input() {
        return _input;
    }

    public double[] get_output() {
        return _output;
    }
}
