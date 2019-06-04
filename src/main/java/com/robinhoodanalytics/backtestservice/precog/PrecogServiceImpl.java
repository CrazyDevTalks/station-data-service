package com.robinhoodanalytics.backtestservice.precog;

import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.models.Predictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component("precogService")
public class PrecogServiceImpl implements PrecogService{
    @Autowired
    private PrecogRepository precogRepo;

    @Autowired
    private static final Logger log = LoggerFactory.getLogger(BacktestServiceApplication.class);

    @Override
    public ResponseEntity savePrediction(Predictions payload) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.TEXT_PLAIN);

        precogRepo.save(payload);
        return new ResponseEntity<>("\"Predictions Added.\"", responseHeaders, HttpStatus.OK);
    }

    @Override
    public ResponseEntity findPrediction(String symbol, LocalDateTime datetime) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        List<Predictions> predictions = precogRepo.findBySymbolAndDate(symbol, datetime);
        if (predictions.size() > 0) {
            return new ResponseEntity<>(predictions, responseHeaders, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("\"Predictions Not Found\"", responseHeaders, HttpStatus.NOT_FOUND);
        }
    }
}
