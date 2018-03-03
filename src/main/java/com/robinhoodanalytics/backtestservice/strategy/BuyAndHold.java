package com.robinhoodanalytics.backtestservice.strategy;

import com.robinhoodanalytics.backtestservice.models.Order;
import com.robinhoodanalytics.backtestservice.models.Quote;
import com.robinhoodanalytics.backtestservice.models.TradingContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuyAndHold implements TradingStrategy {
    private TradingContext context;

    @Override
    public void initialize(TradingContext context) {
        this.context = context;
    }

    @Override
    public void onTick(Map<String, List<Quote>> quotes) {
        List<Order> orders = new ArrayList<>();

        this.context.getStocks().parallelStream().forEach((stockRank) -> {
            List<Quote> stockQuotes = quotes.get(stockRank.getSymbol());
            if (stockQuotes != null && !stockQuotes.isEmpty()) {
                Quote quote = quotes.get(stockRank.getSymbol()).get(stockQuotes.size() - 1);
                int quantity = 1;
                BigDecimal cost = quote.getClose().multiply(new BigDecimal(quantity));
                if (this.context.getCash().compareTo(cost) >= 0) {
                    Order myOrder = new Order(stockRank, quantity, quote.getClose(), Order.Side.buy);
                    orders.add(myOrder);
                }
            }
        });
    }

    @Override
    public void rebalance() {

    }
}
