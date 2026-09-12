package com.dtech.admin.maintenance;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemMaintenanceFilterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 3, 12, 0);

    @Test
    void activatesImmediateAndCurrentMaintenanceWindows() {
        assertTrue(SystemMaintenanceFilter.isActive(true, null, null, NOW));
        assertTrue(SystemMaintenanceFilter.isActive(true, NOW.minusHours(1), NOW.plusHours(1), NOW));
    }

    @Test
    void staysAvailableWhenDisabledOrOutsideTheWindow() {
        assertFalse(SystemMaintenanceFilter.isActive(false, null, null, NOW));
        assertFalse(SystemMaintenanceFilter.isActive(true, NOW.plusMinutes(1), null, NOW));
        assertFalse(SystemMaintenanceFilter.isActive(true, null, NOW.minusMinutes(1), NOW));
    }
}
