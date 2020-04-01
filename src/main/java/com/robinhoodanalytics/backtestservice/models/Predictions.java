package com.robinhoodanalytics.backtestservice.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@CompoundIndex(def = "{'symbol': 1, 'model': 1}")
@Document(collection = "predictions")
public class Predictions {
    @Id
    String id;

    public String symbol;

    public String modelName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public LocalDateTime date;

    public List<PredictionScore> results;
}
