package com.robinhoodanalytics.backtestservice.trainer;

import org.springframework.http.ResponseEntity;

import java.util.Date;

public interface TrainerService {
    ResponseEntity train(String symbol, Date from, Date to, boolean save);
}
