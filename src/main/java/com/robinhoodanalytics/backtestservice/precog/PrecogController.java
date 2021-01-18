package com.robinhoodanalytics.backtestservice.precog;

import com.robinhoodanalytics.backtestservice.models.Predictions;
import com.robinhoodanalytics.backtestservice.models.Quote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/precog")
public class PrecogController {
    @Autowired
    private PrecogService _precogService;

    @RequestMapping(
            value = "/prediction",
            method = RequestMethod.POST)
    ResponseEntity savePrecogData(@RequestBody Predictions payload)
    {
        return _precogService.savePrediction(payload);
    }

    @RequestMapping(
            value = "/prediction",
            method = RequestMethod.GET)
    ResponseEntity findPrecogData(@RequestParam(value = "symbol") String symbol,
                                  @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date date,
                                  @RequestParam(value = "modelName") String modelName)
    {
        LocalDateTime datetime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        return _precogService.findPrediction(symbol, modelName, datetime);
    }
}
