package com.merchantnottingham.littlejohnbacktestservice.quotes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document
public class Quote {

    @Id
    public String symbol;

    @Indexed
    public Calendar date;

    public BigDecimal open;

    public BigDecimal high;

    public BigDecimal low;

    public BigDecimal close;

    public long volume;

    public Quote() {}

    public Quote(String symbol, Calendar date, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, long volume) {
        this.symbol = symbol;
        this.date = date;
        this.open = open;
        this.close = close;
        this.low = low;
        this.high = high;
        this.volume = volume;
    }

    @Override
    public String toString() {
        return "Quote{" +
                "symbol=" + symbol +
                ", date='" + date + '\'' +
                "open=" + open +
                "high=" + high +
                "low=" + low +
                "close=" + close +
                "volume=" + volume +
                '}';
    }
}
