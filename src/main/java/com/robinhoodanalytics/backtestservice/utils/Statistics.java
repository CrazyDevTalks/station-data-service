package com.robinhoodanalytics.backtestservice.utils;

import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import org.apache.commons.math3.stat.StatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class Statistics {
    public static final BigDecimal TWO = new BigDecimal(2);
    private static final Logger log = LoggerFactory.getLogger(BacktestServiceApplication.class);

    public static double[] drawdown(double[] series) {
        double max = Double.MIN_VALUE;
        double ddPct = Double.MAX_VALUE;
        double dd = Double.MAX_VALUE;

        for (double x : series) {
            dd = Math.min(x - max, dd);
            ddPct = Math.min(x / max - 1, ddPct);
            max = Math.max(max, x);
        }

        return new double[]{dd, ddPct};
    }

    public static double sharpe(double[] dailyReturns) {
        return StatUtils.mean(dailyReturns) / Math.sqrt(StatUtils.variance(dailyReturns)) * Math.sqrt(250);
    }

    public static double[] returns(double[] series) {
        if (series.length <= 1) {
            return new double[0];
        }

        double[] returns = new double[series.length - 1];
        for (int i = 1; i < series.length; i++) {
            returns[i - 1] = series[i] / series[i - 1] - 1;
        }

        return returns;
    }

    public static BigDecimal percentDifference(BigDecimal val1, BigDecimal val2) {
        BigDecimal denom = val1.add(val2).divide(TWO,  2, RoundingMode.HALF_EVEN);

        return val1.subtract(val2).abs()
                .divide(denom,  2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentChange(BigDecimal originalValue, BigDecimal newValue) {
        if (originalValue.compareTo(BigDecimal.ZERO) == 0 || newValue.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal num = newValue.subtract(originalValue);
        if (num.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return num.divide(originalValue, 2, RoundingMode.HALF_EVEN);
    }}