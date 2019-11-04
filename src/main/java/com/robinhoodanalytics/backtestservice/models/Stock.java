package com.robinhoodanalytics.backtestservice.models;

public class Stock {
    private String _symbol;

    public Stock(String symbol) {
        _symbol = symbol;
    }

    public String getSymbol() {
        return _symbol;
    }
}
