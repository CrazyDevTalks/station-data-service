package com.robinhoodanalytics.backtestservice.backtest;

import com.robinhoodanalytics.backtestservice.models.Signal;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface BacktestMainService {
    List<Signal> executeMeanReversion(String symbol, Date from, Date to, BigDecimal deviation, int shortTerm, int longTerm) throws Exception;
    List<Signal> buyAndHold(List<String> symbols, Date from, Date t, BigDecimal initialFund);
}
