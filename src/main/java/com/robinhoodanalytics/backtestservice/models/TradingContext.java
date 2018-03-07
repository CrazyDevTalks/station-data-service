package com.robinhoodanalytics.backtestservice.models;

import java.math.BigDecimal;
import java.util.List;

public interface TradingContext {
    List<StockRank> getStocks();
    BigDecimal getInitialFund();
    BigDecimal getCash();
    void addOrder(Order order);
    List<Order> getOrders();
    BigDecimal fillOrder(BigDecimal cost);
}
