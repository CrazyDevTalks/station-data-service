package com.robinhoodanalytics.backtestservice.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

public class RollingAverage {

    private int size;
    private BigDecimal total = new BigDecimal(0.0);
    private int index = 0;
    private BigDecimal samples[];

    public RollingAverage(int size) {
        this.size = size;
        samples = new BigDecimal[size];
        Arrays.fill(samples, BigDecimal.ZERO);
    }

    public void add(BigDecimal x) {
        total = total.subtract(samples[index]);
        samples[index] = x;
        total = total.add(x);
        if (++index == size) {
            index = 0; // cheaper than modulus
        }
    }

    public BigDecimal getAverage() {
        return total.divide(new BigDecimal(size), RoundingMode.HALF_UP);
    }

    public BigDecimal[] getSamples() {
        return samples;
    }
}