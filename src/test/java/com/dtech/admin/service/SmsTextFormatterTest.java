package com.dtech.admin.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmsTextFormatterTest {
    @Test
    void convertsClaimTemplateBreaksToSmsLines() {
        assertEquals(
                "Your medical claim has been successfully submitted under Claim ID: HC/DTECH/NS/2026/0082. "
                        + "We will keep you updated as soon as possible.\n\nThank you!",
                SmsTextFormatter.toPlainText(
                        "Your medical claim has been successfully submitted under Claim ID: "
                                + "HC/DTECH/NS/2026/0082. We will keep you updated as soon as possible."
                                + "<br><br> Thank you!"));
    }

    @Test
    void handlesBreakVariantsAndHtmlEntitiesWithoutChangingPlainText() {
        assertEquals("One\nTwo\nThree & four",
                SmsTextFormatter.toPlainText("<p>One<BR/>Two<br class=\"x\">Three &amp; four</p>"));
        assertEquals("Your OTP is 123456", SmsTextFormatter.toPlainText("Your OTP is 123456"));
    }
}
