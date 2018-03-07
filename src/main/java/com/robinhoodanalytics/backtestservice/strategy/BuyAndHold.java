package com.robinhoodanalytics.backtestservice.strategy;

import com.robinhoodanalytics.backtestservice.models.Order;
import com.robinhoodanalytics.backtestservice.models.Quote;
import com.robinhoodanalytics.backtestservice.models.TradingContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class BuyAndHold implements TradingStrategy {
    private TradingContext context;

    @Override
    public void initialize(TradingContext context) {
        this.context = context;
    }

    @Override
    public void onTick(Date date, Map<String, Quote> today) {
        this.context.getStocks().parallelStream().forEach((stockRank) -> {
            Quote quote = today.get(stockRank.getSymbol());
            int quantity = 1;
            BigDecimal cost = quote.getClose().multiply(new BigDecimal(quantity));
            if (this.context.getCash().compareTo(cost) >= 0) {
                Order myOrder = new Order(stockRank, quantity, quote.getClose(), Order.Side.buy, quote.getDate());
                this.context.addOrder(myOrder);
             }
        });

        for (int i = this.context.getOrders().size(); i >= 0; i--) {
            Order curr = this.context.getOrders().get(i);
            if (date.compareTo(curr.getDate()) != 0) {
                break;
            } else {
                BigDecimal cost = curr.getPrice().multiply(new BigDecimal(curr.getQuantity()));
                if (this.context.getCash().compareTo(cost) >= 0) {
                    this.context.getOrders().get(i).fillOrder();
                    this.context.fillOrder(cost);
                }
            }
        }
    }

    @Override
    public void rebalance() {
    }

    public TradingContext getContext() {
        return this.context;
    }
}
