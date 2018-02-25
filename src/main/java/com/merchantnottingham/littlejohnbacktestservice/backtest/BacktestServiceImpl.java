package com.merchantnottingham.littlejohnbacktestservice.backtest;

import com.merchantnottingham.littlejohnbacktestservice.models.Signal;
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

    public ResponseEntity<List<Signal>> trainMeanReversion(String symbol,
                                                           Date from,
                                                           Date to,
                                                           int shortTermStart,
                                                           int longTermStart,
                                                           int shortTermEnd,
                                                           int longTermEnd
    )
    {
        return new ResponseEntity<List<Signal>>(HttpStatus.NOT_FOUND);
    }

    public void run()
    {

    }
}
