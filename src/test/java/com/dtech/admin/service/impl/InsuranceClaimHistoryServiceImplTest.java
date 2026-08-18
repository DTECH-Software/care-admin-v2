package com.dtech.admin.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InsuranceClaimHistoryServiceImplTest {

    @Test
    void mapsNestedApprovalLevelSortToBasicEntityAttribute() {
        assertEquals("approvalLevel",
                InsuranceClaimHistoryServiceImpl.normalizeSortColumn("approvalLevel.description"));
        assertEquals("approvalLevel",
                InsuranceClaimHistoryServiceImpl.normalizeSortColumn("approvalLevelDescription"));
    }

    @Test
    void preservesValidSortColumnsAndDefaultsBlankValues() {
        assertEquals("requestId", InsuranceClaimHistoryServiceImpl.normalizeSortColumn("requestId"));
        assertEquals("lastModifiedDate", InsuranceClaimHistoryServiceImpl.normalizeSortColumn(" "));
    }
}
