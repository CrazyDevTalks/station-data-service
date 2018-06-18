package com.robinhoodanalytics.backtestservice.models;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

public class Signal {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date date;

    private Action action;

    private BigDecimal deviation;

    private BigDecimal shortTermAverage;

    private BigDecimal longTermAverage;

    private BigDecimal volumeChange;

    private BigDecimal close;

    private long volume;

    public Signal(Date date, Action action, BigDecimal deviation,
                  BigDecimal shortTermAverage, BigDecimal longTermAverage,
                  BigDecimal volumeChange, BigDecimal close, long volume) {
        this.date = date;
        this.action = action;
        this.deviation = deviation;
        this.shortTermAverage = shortTermAverage;
        this.longTermAverage = longTermAverage;
        this.volumeChange = volumeChange;
        this.close = close;
        this.volume = volume;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public BigDecimal getDeviation() {
        return deviation;
    }

    public void setDeviation(BigDecimal deviation) {
        this.deviation = deviation;
    }

    public BigDecimal getShortTermAverage() {
        return shortTermAverage;
    }

    public void setShortTermAverage(BigDecimal shortTermAverage) {
        this.shortTermAverage = shortTermAverage;
    }

    public BigDecimal getLongTermAverage() {
        return longTermAverage;
    }

    public void setLongTermAverage(BigDecimal longTermAverage) {
        this.longTermAverage = longTermAverage;
    }

    public BigDecimal getClose() {
        return close;
    }

    public void setClose(BigDecimal close) {
        this.close = close;
    }

    public BigDecimal getVolumeChange() { return volumeChange; }

    public long getVolume() { return volume; }
 }
