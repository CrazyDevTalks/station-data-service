package com.robinhoodanalytics.backtestservice.models;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

public class Order {
    public enum Side {
        buy, sell;
    }

    private Date date;
    private Stock stock;
    private int quantity;
    private BigDecimal price;
    private Side side;

    private boolean filled;

    public Order(Stock stock, int quantity, BigDecimal price, Side side, Date date) {
        this.stock = stock;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
        this.filled = false;
        this.date = date;
    }

    public void fillOrder() {
        this.filled = true;
    }

    public boolean isFilled() {
        return this.filled;
    }

    public Date getDate() {
        return this.date;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }
}
