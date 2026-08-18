package com.dtech.admin.event;

/**
 * Raised inside the dependent approval transaction. The notification listener
 * processes it only after that transaction commits successfully.
 */
public record DependentApprovedEvent(Long dependentId, String approvedBy) {
}
