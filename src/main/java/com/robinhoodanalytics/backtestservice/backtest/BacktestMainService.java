package com.robinhoodanalytics.backtestservice.backtest;

import com.robinhoodanalytics.backtestservice.models.Signal;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface BacktestMainService {
    List<Signal> getMeanReversionTimeline(String symbol, Date from, Date to, int shortTerm, int longTerm, int bbandPeriod) throws Exception;
    BacktestMainServiceImpl.BacktestSummary getMeanReversionResults(String symbol, Date from, Date to, BigDecimal deviation, int shortTerm, int longTerm, int bbandPeriod) throws Exception;
    List<Signal> buyAndHold(List<String> symbols, Date from, Date t, BigDecimal initialFund);
}
