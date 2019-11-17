package com.robinhoodanalytics.backtestservice.strategy;

import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.models.Action;
import com.robinhoodanalytics.backtestservice.models.BBandOptions;
import com.robinhoodanalytics.backtestservice.models.BollingerBand;
import com.robinhoodanalytics.backtestservice.models.Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class BbandMfi implements Strategy{
    private static final Logger log = LoggerFactory.getLogger(BacktestServiceApplication.class);
    public double[] high;
    public double[] low;
    public double[] close;
    public long[] volume;
    public double[] bbandClose;
    public int mfiPeriod;
    public int bbandPeriod;

    @Override
    public Signal onTick(Date date) {
        Signal sig = null;
        String apiUrl = "http://localhost:9000/api/backtest/mfi";

        MfiPayload body = new MfiPayload(this.high, this.low, this.close, this.volume, this.mfiPeriod);

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<MfiPayload> entity = new HttpEntity<>(body);

        ResponseEntity<List<List<BigDecimal>>> mfiResponse = restTemplate.exchange(apiUrl,
                HttpMethod.POST, entity, new ParameterizedTypeReference<List<List<BigDecimal>>>() {
                });

        BigDecimal closePrice = new BigDecimal(close[close.length - 1]);

        BollingerBand bands = this.getBollingerBand(this.bbandClose);

        if (closePrice.compareTo(bands.lower) < 0 && mfiResponse.getBody().get(0).get(0).doubleValue() < 15) {
            sig = new Signal(date, Action.STRONGBUY, closePrice, volume[volume.length - 1]);
        }

        if (closePrice.compareTo(bands.upper) > 0 && mfiResponse.getBody().get(0).get(0).doubleValue() > 30) {
            sig = new Signal(date, Action.STRONGSELL, closePrice, volume[volume.length - 1]);
        }
        return sig;
    }

    private List<List<BigDecimal>> requestBBand(double[] real, int period) {
        BBandOptions body = new BBandOptions(real, period, 2);
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<BBandOptions> entity = new HttpEntity<>(body);

        ResponseEntity<List<List<BigDecimal>>> rateResponse =
                restTemplate.exchange("http://localhost:9000/api/backtest/bbands",
                        HttpMethod.POST, entity, new ParameterizedTypeReference<List<List<BigDecimal>>>() {
                        });

        List<List<BigDecimal>> bbands = rateResponse.getBody();

        return bbands;
    }

    private BollingerBand getBollingerBand(double[] bband) {
        List<List<BigDecimal>> band = requestBBand(bband, bband.length);

        BigDecimal lower = band.get(0).get(0);
        BigDecimal mid = band.get(1).get(0);
        BigDecimal upper = band.get(2).get(0);
        return new BollingerBand(lower, mid, upper);
    }
}
