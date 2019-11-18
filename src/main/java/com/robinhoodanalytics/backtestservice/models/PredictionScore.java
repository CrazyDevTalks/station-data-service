package com.robinhoodanalytics.backtestservice.models;

import java.math.BigDecimal;

public class PredictionScore {
    public int guesses;
    public int correct;
    public BigDecimal score;
    public BigDecimal nextOutput;
}
