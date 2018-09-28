package com.robinhoodanalytics.backtestservice.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.robinhoodanalytics.backtestservice.utils.DateParser;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "aggregated_quotes")
public class AggregatedQuote {
    @Id
    String id;

    public String symbol;

    @DateTimeFormat(pattern = "dd/MM/yyyy")
    public LocalDate date;
    public double[] input;
    public double[] output;

    public AggregatedQuote(String symbol, LocalDate date, double[] input, double[] output) {
        this.symbol = symbol;
        this.date = date;
        this.input = input;
        this.output = output;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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

    public LocalDate getDate() {
        return date;
    }

    public double[] getInput() {
        return input;
    }

    public double[] getOutput() {
        return output;
    }

    @Override
    public String toString() {
        return "Quote{" +
                "symbol=" + symbol +
                ", date='" + date + '\'' +
                "input=" + Arrays.toString(input) +
                "output=" + Arrays.toString(output) +
                "}";
    }
}
