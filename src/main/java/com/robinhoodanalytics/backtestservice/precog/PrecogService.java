package com.robinhoodanalytics.backtestservice.precog;

import com.robinhoodanalytics.backtestservice.models.Predictions;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public interface PrecogService {
    ResponseEntity<int[]> retrievePrediction(double[] input);
    ResponseEntity savePrediction(Predictions payload);
    ResponseEntity findPrediction(String symbol, String modelName, LocalDateTime datetime);
}
