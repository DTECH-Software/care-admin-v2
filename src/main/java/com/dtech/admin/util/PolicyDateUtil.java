package com.dtech.admin.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** Calendar-date operations for Sri Lankan policy and quarter boundaries. */
public final class PolicyDateUtil {

    private static final ZoneId POLICY_ZONE = ZoneId.of("Asia/Colombo");

    private PolicyDateUtil() {
    }

    public static java.sql.Date todaySqlDate() {
        return java.sql.Date.valueOf(LocalDate.now(POLICY_ZONE));
    }

    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return date.toInstant().atZone(POLICY_ZONE).toLocalDate();
    }

    public static java.sql.Date toSqlDate(Date date) {
        LocalDate localDate = toLocalDate(date);
        return localDate == null ? null : java.sql.Date.valueOf(localDate);
    }

    public static boolean contains(Date from, Date to, Date value) {
        LocalDate start = toLocalDate(from);
        LocalDate end = toLocalDate(to);
        LocalDate day = toLocalDate(value);
        return start != null && end != null && day != null
                && !day.isBefore(start) && !day.isAfter(end);
    }

    public static boolean isBefore(Date left, Date right) {
        LocalDate first = toLocalDate(left);
        LocalDate second = toLocalDate(right);
        return first != null && second != null && first.isBefore(second);
    }

    public static boolean overlaps(Date leftFrom, Date leftTo, Date rightFrom, Date rightTo) {
        LocalDate aFrom = toLocalDate(leftFrom);
        LocalDate aTo = toLocalDate(leftTo);
        LocalDate bFrom = toLocalDate(rightFrom);
        LocalDate bTo = toLocalDate(rightTo);
        return aFrom != null && aTo != null && bFrom != null && bTo != null
                && !aTo.isBefore(bFrom) && !aFrom.isAfter(bTo);
    }
}
