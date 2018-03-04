package com.robinhoodanalytics.backtestservice.models;

public class Stock {
    private String _symbol;

    protected Stock(String symbol) {
        _symbol = symbol;
    }

    public String getSymbol() {
        return _symbol;
    }
}
