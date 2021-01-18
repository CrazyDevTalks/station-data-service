package com.robinhoodanalytics.backtestservice.trainer;

import com.robinhoodanalytics.backtestservice.models.AggregatedQuote;
import com.robinhoodanalytics.backtestservice.models.Quote;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public interface TrainerService {
    List<Quote> sanitizeQuotes(String symbol, Date from, Date to);
    AggregatedQuote[] convertTrainingData(String symbol, Date from, Date to, boolean outputClosePrice);
    ResponseEntity train(String symbol, Date from, Date to, boolean save, boolean outputClosePrice);
    ResponseEntity findTrainingData(String symbol, LocalDate from, LocalDate to, boolean save);
    void trainHmmModel();
}
