package com.robinhoodanalytics.backtestservice.models;

import java.util.ArrayList;

public class AggregatedQuote {
    public String[] _featureNames;
    public int[] _input;

    public AggregatedQuote(String[] featureNames, int[] input) {
        this._featureNames = featureNames;
        this._input = input;
    }
}
