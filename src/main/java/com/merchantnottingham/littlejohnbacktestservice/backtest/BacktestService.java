package com.merchantnottingham.littlejohnbacktestservice.backtest;

import com.merchantnottingham.littlejohnbacktestservice.models.Signal;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface BacktestService {
    ResponseEntity<List<Signal>> executeMeanReversion(String symbol, Date from, Date to, BigDecimal deviation, int shortTerm, int longTerm);
}
