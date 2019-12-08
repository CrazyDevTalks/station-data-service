package com.robinhoodanalytics.backtestservice.strategy;

import com.robinhoodanalytics.backtestservice.models.Action;
import com.robinhoodanalytics.backtestservice.models.Signal;
import com.robinhoodanalytics.backtestservice.utils.RollingAverage;
import com.robinhoodanalytics.backtestservice.utils.Statistics;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Deque;

public class MovingAverageCrossover implements Strategy {
    public RollingAverage shortTermWindow;
    public RollingAverage longTermWindow;
    public Deque<BigDecimal> shortTermAverageHistory;
    @Override
    public Signal onTick(Date date) {
        BigDecimal shortAvg = shortTermWindow.getAverage();
        BigDecimal longAvg = longTermWindow.getAverage();

        BigDecimal pctChange = Statistics.percentChange(shortAvg,
                    longAvg);
        if (pctChange.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal firstItem = shortTermAverageHistory.peekFirst();
            if (firstItem.compareTo(shortAvg) > 0) {
                return new Signal(date, Action.STRONGSELL);
            } else if (firstItem.compareTo(shortAvg) < 0) {
                return new Signal(date, Action.STRONGBUY);
            }
        }

        return new Signal(date, Action.INDETERMINANT);
    }
}
