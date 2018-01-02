package com.merchantnottingham.littlejohnbacktestservice.quotes;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Calendar;
import java.util.List;

public interface QuoteRepository extends MongoRepository<Quote, String>{

    public List<Quote> findBySymbolAndDateBetween(String symbol, Calendar from, Calendar to);
}
