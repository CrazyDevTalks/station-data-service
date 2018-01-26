package com.merchantnottingham.littlejohnbacktestservice.backtest;

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

    private BigDecimal close;

    private BigDecimal shortTermTotal;

    private BigDecimal longTermTotal;

    private BigDecimal total;

    public Signal(Date date, Action action, BigDecimal deviation, BigDecimal shortTermAverage, BigDecimal longTermAverage, BigDecimal close, BigDecimal shortTermTotal, BigDecimal longTermTotal, BigDecimal total) {
        this.date = date;
        this.action = action;
        this.deviation = deviation;
        this.shortTermAverage = shortTermAverage;
        this.longTermAverage = longTermAverage;
        this.close = close;
        this.shortTermTotal = shortTermTotal;
        this.longTermTotal = longTermTotal;
        this.total = total;
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

    public BigDecimal getShortTermTotal() {
        return shortTermTotal;
    }

    public void setShortTermTotal(BigDecimal shortTermTotal) {
        this.shortTermTotal = shortTermTotal;
    }

    public BigDecimal getLongTermTotal() {
        return longTermTotal;
    }

    public void setLongTermTotal(BigDecimal longTermTotal) {
        this.longTermTotal = longTermTotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
