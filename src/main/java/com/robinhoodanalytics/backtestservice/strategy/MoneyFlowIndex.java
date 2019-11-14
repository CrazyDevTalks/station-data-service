package com.robinhoodanalytics.backtestservice.strategy;

import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.models.Action;
import com.robinhoodanalytics.backtestservice.models.Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class MoneyFlowIndex {
    private static final Logger log = LoggerFactory.getLogger(BacktestServiceApplication.class);

    public Signal onTick(Date date, double[] high, double[] low, double[] close, long[] volume, int period) {
        String apiUrl = "http://localhost:9000/api/backtest/mfi";

        MfiPayload body = new MfiPayload(high, low, close, volume, period);

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<MfiPayload> entity = new HttpEntity<>(body);

        ResponseEntity<List<List<BigDecimal>>> mfiResponse = restTemplate.exchange(apiUrl,
                HttpMethod.POST, entity, new ParameterizedTypeReference<List<List<BigDecimal>>>() {
                });

        if (mfiResponse.getBody().get(0).get(0).doubleValue() < 15) {
            return new Signal(date, Action.STRONGBUY, new BigDecimal(close[close.length - 1]), volume[volume.length - 1]);
        } else if (mfiResponse.getBody().get(0).get(0).doubleValue() > 30) {
            return new Signal(date, Action.STRONGSELL, new BigDecimal(close[close.length - 1]), volume[volume.length - 1]);
        }

        return null;
    }
}
