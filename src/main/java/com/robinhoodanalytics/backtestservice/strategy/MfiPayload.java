package com.robinhoodanalytics.backtestservice.strategy;

public class MfiPayload {
    public double[] high;
    public double[] low;
    public double[] close;
    public long[] volume;
    public int period;

    public MfiPayload(double[] high, double[] low, double[] close, long[] volume, int period) {
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.period = period;
    }
}
