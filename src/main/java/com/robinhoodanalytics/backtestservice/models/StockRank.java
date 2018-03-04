package com.robinhoodanalytics.backtestservice.models;

public class StockRank extends Stock {
    private double _weight;
    
    public StockRank(String symbol, double weight) {
        super(symbol);
        _weight = weight;
    }

    public double getWeight() {
        return _weight;
    }
}
