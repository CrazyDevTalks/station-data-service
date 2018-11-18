package com.robinhoodanalytics.backtestservice.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "intraday_quotes")
public class IntradayQuote {

    @Id
    String id;

    @Indexed
    public String symbol;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public LocalDateTime date;

    public BigDecimal open;

    public BigDecimal high;

    public BigDecimal low;

    public BigDecimal close;

    public long volume;

    public IntradayQuote(String symbol, LocalDateTime date, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, long volume) {
        this.symbol = symbol;
        this.date = date;
        this.open = open;
        this.close = close;
        this.low = low;
        this.high = high;
        this.volume = volume;
    }
    public IntradayQuote() {}
}
