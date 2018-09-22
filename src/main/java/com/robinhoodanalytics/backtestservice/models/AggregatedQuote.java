package com.robinhoodanalytics.backtestservice.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.robinhoodanalytics.backtestservice.utils.DateParser;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "aggregated_quotes")
public class AggregatedQuote {
    @Id
    String id;

    public String symbol;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public Date date;
    public int day;
    public double[] input;
    public double[] output;

    public AggregatedQuote(String symbol, Date date, double[] input, double[] output) {
        this.symbol = symbol;
        this.date = date;
        this.input = input;
        this.output = output;
        this.day = DateParser.getDayOfWeek(date);
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public void setOutput(double[] output) {
        this.output = output;
    }

    public void setInput(double[] input) {
        this.input = input;
    }

    public String getSymbol() {
        return symbol;
    }

    public Date getDate() {
        return date;
    }

    public int getDay() {
        return day;
    }

    public double[] getInput() {
        return input;
    }

    public double[] getOutput() {
        return output;
    }
}
