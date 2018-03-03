package com.robinhoodanalytics.backtestservice.models;

import java.math.BigDecimal;

public class Order {
    public enum Side {
        buy, sell;
    }

    private Stock _stock;
    private int _quantity;
    private BigDecimal _price;
    private Side _side;

    public Order(Stock stock, int quantity, BigDecimal price, Side side) {
        _stock = stock;
        _quantity = quantity;
        _price = price;
        _side = side;
    }
}
