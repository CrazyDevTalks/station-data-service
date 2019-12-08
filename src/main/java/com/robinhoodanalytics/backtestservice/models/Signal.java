package com.robinhoodanalytics.backtestservice.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "signals")
public class Signal {

    @Id
    String id;

    @Indexed
    public String symbol;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date date;

    private Action action;

    private BigDecimal deviation;

    private BigDecimal shortTermAverage;

    private BigDecimal longTermAverage;

    private BigDecimal volumeChange;

    private BigDecimal close;

    private long volume;

    public String bbandPosition;

    public double mfi;

    public BigDecimal oneWeekGain;

    public BigDecimal oneMonthGain;

    public int shortTermSize;

    public int longTermSize;

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

    public Signal(Date date, Action action) {
        this.date = date;
        this.action = action;
    }

    public Signal(Date date, Action action, BigDecimal close, long volume) {
        this.date = date;
        this.action = action;
        this.close = close;
        this.volume = volume;
    }

    public Signal(Date date, Action action, BigDecimal close, long volume,
                  String bbandPosition, double mfi) {
        this.date = date;
        this.action = action;
        this.close = close;
        this.volume = volume;
        this.bbandPosition = bbandPosition;
        this.mfi = mfi;
    }


    public Signal() {
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

    public void setVolume(long volume) {  this.volume = volume; }
}
