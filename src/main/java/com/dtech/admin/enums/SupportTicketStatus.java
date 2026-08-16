package com.dtech.admin.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SupportTicketStatus {
    OPEN("Open"),
    IN_PROGRESS("In progress"),
    WAITING_FOR_CLIENT("Waiting for client"),
    RESOLVED("Resolved"),
    CLOSED("Closed"),
    REOPENED("Reopened");

    private final String description;
}
