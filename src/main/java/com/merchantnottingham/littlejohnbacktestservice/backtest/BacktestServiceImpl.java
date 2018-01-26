package com.merchantnottingham.littlejohnbacktestservice.backtest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class BacktestServiceImpl
    implements BacktestService
{
    @Override
    public ResponseEntity<List<Signal>> executeMeanReversion(String symbol, Date from, Date to, BigDecimal deviation, int shortTerm, int longTerm)
    {
        return new ResponseEntity<List<Signal>>(HttpStatus.NOT_FOUND);
    }
}
