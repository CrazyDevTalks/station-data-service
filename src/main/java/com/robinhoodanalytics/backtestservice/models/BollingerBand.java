package com.robinhoodanalytics.backtestservice.models;

import java.math.BigDecimal;

public class BollingerBand {
    public BigDecimal lower;
    public BigDecimal mid;
    public BigDecimal upper;

    public BollingerBand() {}
    public BollingerBand(BigDecimal lower, BigDecimal mid, BigDecimal upper) {
        this.lower = lower;
        this.mid = mid;
        this.upper = upper;
    }

    @Override
    public String toString() {
        return "BollingerBand{" +
                "lower=" + lower +
                ", mid=" + mid +
                ", upper=" + upper +
                '}';
    }
}
