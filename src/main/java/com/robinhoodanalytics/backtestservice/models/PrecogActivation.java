package com.robinhoodanalytics.backtestservice.models;

public class PrecogActivation {
    public double[] input;
    public boolean round;

    public PrecogActivation(double[] input, boolean round) {
        this.input = input;
        this.round = round;
    }
}
