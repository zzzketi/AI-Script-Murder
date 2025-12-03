package com.ai.aiscriptmurde.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    private static final long ONE_MINUTE_IN_MILLIS = 60 * 1000;
    private static final long ONE_HOUR_IN_MILLIS = 60 * ONE_MINUTE_IN_MILLIS;
    private static final long ONE_DAY_IN_MILLIS = 24 * ONE_HOUR_IN_MILLIS;

    public static String getFriendlyTimeSpan(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < ONE_MINUTE_IN_MILLIS) {
            return "刚刚";
        } else if (diff < ONE_HOUR_IN_MILLIS) {
            return (diff / ONE_MINUTE_IN_MILLIS) + "分钟前";
        }

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        // 是今天
        if (timestamp >= today.getTimeInMillis()) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        }

        // 是昨天
        today.add(Calendar.DAY_OF_MONTH, -1);
        if (timestamp >= today.getTimeInMillis()) {
            return "昨天 " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        }
        
        // 是本周
        today.add(Calendar.DAY_OF_MONTH, 1); // Reset to today
        today.set(Calendar.DAY_OF_WEEK, today.getFirstDayOfWeek());
        if (timestamp >= today.getTimeInMillis()) {
             return new SimpleDateFormat("E", Locale.getDefault()).format(new Date(timestamp)); // E.g., "周三"
        }

        // 更早
        return new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(new Date(timestamp));
    }
}