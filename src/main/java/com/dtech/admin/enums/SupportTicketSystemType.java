package com.dtech.admin.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SupportTicketSystemType {
    WECARE_ADMIN("WeCare Admin"),
    WECARE_APP("WeCare App");

    private final String description;
}
