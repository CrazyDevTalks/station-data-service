package com.robinhoodanalytics.backtestservice.backtest;

import com.robinhoodanalytics.backtestservice.backtest.models.PrecogBacktestResults;
import com.robinhoodanalytics.backtestservice.models.BacktestSummary;
import com.robinhoodanalytics.backtestservice.models.Signal;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface BacktestMainService {
    PrecogBacktestResults[] backtestRnn(String symbol, Date from, Date to);
    List<Signal> getMeanReversionTimeline(String symbol, Date from, Date to, int shortTerm, int longTerm) throws Exception;
    BacktestSummary getMeanReversionResults(String symbol, Date from, Date to, BigDecimal deviation, int shortTerm, int longTerm) throws Exception;
    List<Signal> buyAndHold(List<String> symbols, Date from, Date t, BigDecimal initialFund);
    BacktestSummary backtestStrategy(String symbol, String strategy, Date from, Date to, double[] settings) throws Exception;
    ResponseEntity trainSignals(String symbol, String strategy, Date from, Date to);
}
