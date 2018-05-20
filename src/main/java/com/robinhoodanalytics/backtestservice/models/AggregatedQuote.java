package com.robinhoodanalytics.backtestservice.models;

import java.util.Date;

public class AggregatedQuote {
    public double[] input;
    public double[] output;

    public AggregatedQuote(double[] input, double[] output) {
        this.input = input;
        this.output = output;
    }

    public void setOutput(double[] output) {
        this.output = output;
    }

    public void setInput(double[] input) {
        this.input = input;
    }

    public double[] getInput() {
        return input;
    }

    public double[] getOutput() {
        return output;
    }
}
