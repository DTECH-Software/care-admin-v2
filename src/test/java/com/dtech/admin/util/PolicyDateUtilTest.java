package com.dtech.admin.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyDateUtilTest {

    @Test
    void includesEntireFinalPolicyDay() {
        Date from = java.sql.Date.valueOf("2026-08-13");
        Date to = java.sql.Date.valueOf("2027-08-12");

        assertTrue(PolicyDateUtil.contains(from, to,
                Date.from(Instant.parse("2027-08-12T18:29:59.999Z"))));
        assertFalse(PolicyDateUtil.contains(from, to,
                Date.from(Instant.parse("2027-08-12T18:30:00Z"))));
    }

    @Test
    void usesSriLankaDateForSqlLookup() {
        assertEquals(LocalDate.of(2027, 8, 12), PolicyDateUtil.toSqlDate(
                Date.from(Instant.parse("2027-08-12T18:29:59Z"))).toLocalDate());
    }
}
