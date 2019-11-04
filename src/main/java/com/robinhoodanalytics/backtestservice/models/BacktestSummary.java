package com.robinhoodanalytics.backtestservice.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.*;

@Document(collection = "backtest_summary")
public class BacktestSummary {
    @Id
    String id;

    @Indexed
    private String symbol;

    public String algo;
    public int totalTrades = 0;
    public BigDecimal total = BigDecimal.ZERO;
    public BigDecimal invested = BigDecimal.ZERO;
    public BigDecimal returns = BigDecimal.ZERO;
    public long lastVolume;
    public BigDecimal lastPrice;
    public Action recommendation;
    public Deque<BigDecimal> buys = new ArrayDeque<>();
    public ArrayList<Order> orderHistory = new ArrayList<>();
    public Date startDate;
    public Date endDate;
    public List<Signal> signals;
}

