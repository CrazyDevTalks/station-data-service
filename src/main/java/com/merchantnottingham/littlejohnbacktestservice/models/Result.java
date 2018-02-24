package com.merchantnottingham.littlejohnbacktestservice.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.merchantnottingham.littlejohnbacktestservice.utils.Statistics;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "result")
public class Result {
    private String _backtestName;
    private double _profitLoss;
    private double[] _accountHistory;
    private List<Order> _orders;
    private double _initialFund;
    private double _finalValue;
    private double _commissions;

    public Result(String backtestName, double profitLoss, double[] accountHistory, List<Order> orders, double initialFund, double finalValue, double commisions) {
        _backtestName = backtestName;
        _profitLoss = profitLoss;
        _accountHistory = accountHistory;
        _orders = orders;
        _initialFund = initialFund;
        _finalValue = finalValue;
        _commissions = commisions;
    }

    public double getInitialFund() {
        return _initialFund;
    }

    public double getFinalValue() {
        return _finalValue;
    }

    public double getReturn() {
        return _finalValue / _initialFund - 1;
    }

    public double getAnnualizedReturn() {
        return getReturn() * 250 / getDaysCount();
    }

    public double getSharpe() {
        return Statistics.sharpe(Statistics.returns(_accountHistory));
    }

    public double getMaxDrawdown() {
        return Statistics.drawdown(_accountHistory)[0];
    }

    public double getMaxDrawdownPercent() {
        return Statistics.drawdown(_accountHistory)[1];
    }

    public int getDaysCount() {
        return _accountHistory.length;
    }

    public double getPl() {
        return _profitLoss;
    }

    public double getCommissions() {
        return _commissions;
    }

    public List<Order> getOrders() {
        return _orders;
    }
}
