package com.dtech.admin.event;

/**
 * Raised inside the civil-status approval transaction and handled only after
 * that transaction commits successfully.
 */
public record CivilStatusApprovedEvent(Long civilStatusRequestId, String approvedBy) {
}
