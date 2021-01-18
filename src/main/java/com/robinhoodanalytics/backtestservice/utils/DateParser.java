package com.robinhoodanalytics.backtestservice.utils;

import java.util.Calendar;
import java.util.Date;

public class DateParser {
    public static Date toTradeDay(Date date, int modifier, boolean roundUp) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND,0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.add(Calendar.HOUR_OF_DAY, modifier);

        int addDay = 0;
        if ((cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)) {
            if (roundUp) {
                addDay = 2;
            } else {
                addDay = -1;
            }
        } else if ((cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)){
            if (roundUp) {
                addDay = 1;
            } else {
                addDay = -2;
            }
        }

        cal.add(Calendar.DATE, addDay);
        return cal.getTime();
    }

    public static int getDayOfWeek(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        return cal.get(Calendar.DAY_OF_WEEK);
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
