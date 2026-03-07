/**
 * User: Himal_J
 * Date: 2/22/2025
 * Time: 8:15 PM
 * <p>
 */

package com.dtech.admin.util;

import lombok.extern.log4j.Log4j2;

import java.util.function.Predicate;

@Log4j2
public class StringUtil {
    public static int countCharsByConditions(String str, Predicate<Character> predicate) {
        log.info("called count by char method {}", str);
        try {
            int count = 0;
            for(Character c : str.toCharArray() ) {
                if(predicate.test(c)) {
                    count++;
                }
            }
            return count;
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    public static int getCharCount(String str) {
        log.info("called count by get char count method {}", str);
        try {
            int count = 0;
            for(Character c : str.toCharArray() ) {
                count++;
            }
            return count;
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    public static String ifNotOrEmpty(String str) {
        return str == null || str.isEmpty() ? "" : str;
    }

    public static String maskString(String str) {
        log.info("called MaskString method {}", str);
        try {
            int length = str.length();
            return "*".repeat(length - 4) + str.substring(length - 4);
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}
