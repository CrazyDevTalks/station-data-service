package com.robinhoodanalytics.backtestservice.utils;

import java.util.Calendar;
import java.util.Date;

public class DateParser {
    public static Date toTradeDay(Date date, int modifier) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND,0);
        cal.set(Calendar.HOUR_OF_DAY, modifier);

        if ((cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)) {
            cal.add(Calendar.DATE, -1);
        } else if ((cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)){
            cal.add(Calendar.DATE, -2);
        }
        return cal.getTime();
    }

    public static Date standardizeDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND,0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        return cal.getTime();
    }
    public static double estimateTradeDays(double days) {
        double workDaysPerWeek = 5.0 / 7.0;
        double holidays = 9.0;
        return Math.ceil((days * workDaysPerWeek) - holidays);
    }

    public static int compareTradeDays(Date requestedDate, Date foundDate) {
        return standardizeDate(requestedDate).compareTo(standardizeDate(foundDate));
    }
}
