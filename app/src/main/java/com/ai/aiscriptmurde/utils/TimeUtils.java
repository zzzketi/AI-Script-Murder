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

        if (timestamp >= today.getTimeInMillis()) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        }

        today.add(Calendar.DAY_OF_MONTH, -1);
        if (timestamp >= today.getTimeInMillis()) {
            return "昨天 " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        }
        
        today.add(Calendar.DAY_OF_MONTH, 1);
        today.set(Calendar.DAY_OF_WEEK, today.getFirstDayOfWeek());
        if (timestamp >= today.getTimeInMillis()) {
             return new SimpleDateFormat("E", Locale.getDefault()).format(new Date(timestamp));
        }

        return new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(new Date(timestamp));
    }

    /**
     * 🔥 新增：为搜索结果定制的时间格式
     */
    public static String getSearchItemTime(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }

        Calendar msgCal = Calendar.getInstance();
        msgCal.setTimeInMillis(timestamp);

        Calendar nowCal = Calendar.getInstance();

        if (msgCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) {
            // 如果是今年，显示 月-日
            return new SimpleDateFormat("MM-dd", Locale.getDefault()).format(new Date(timestamp));
        } else {
            // 如果是往年，显示 年-月-日
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(timestamp));
        }
    }
}