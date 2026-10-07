package com.dtech.admin.service;

import org.springframework.web.util.HtmlUtils;

import java.util.regex.Pattern;

/** Converts HTML-formatted notification templates into plain SMS text. */
public final class SmsTextFormatter {
    private static final Pattern BREAK = Pattern.compile("(?i)<br\\b[^>]*>");
    private static final Pattern BLOCK_END = Pattern.compile("(?i)</(?:p|div|li)\\s*>");
    private static final Pattern TAG = Pattern.compile("</?[a-zA-Z][^>]*>");
    private static final Pattern LINE_PADDING = Pattern.compile("[ \\t]*\\n[ \\t]*");

    private SmsTextFormatter() {
    }

    public static String toPlainText(String message) {
        if (message == null) {
            return "";
        }
        String text = HtmlUtils.htmlUnescape(message).replace("\r\n", "\n").replace('\r', '\n');
        text = BREAK.matcher(text).replaceAll("\n");
        text = BLOCK_END.matcher(text).replaceAll("\n");
        text = TAG.matcher(text).replaceAll("");
        return LINE_PADDING.matcher(text.replace('\u00a0', ' ')).replaceAll("\n").strip();
    }
}
