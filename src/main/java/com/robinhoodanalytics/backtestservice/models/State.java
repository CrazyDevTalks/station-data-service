package com.robinhoodanalytics.backtestservice.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "states")
public class State {
    @Id
    String id;

    @Indexed
    private String symbol;

    public State() {}

}
