package com.dtech.admin.util;

import lombok.extern.log4j.Log4j2;

import java.util.Random;

@Log4j2
public class RandomGeneratorUtil {

    public static String getRandom6DigitNumber() {
        try {
            log.error("called random 6 digit generation number");
            Random rnd = new Random();
            int number = rnd.nextInt(999999);
            return String.format("%06d", number);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}