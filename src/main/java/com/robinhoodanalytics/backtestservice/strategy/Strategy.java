package com.robinhoodanalytics.backtestservice.strategy;

import com.robinhoodanalytics.backtestservice.models.Signal;

import java.util.Date;

public interface Strategy {
    public Signal onTick(Date date);
}
