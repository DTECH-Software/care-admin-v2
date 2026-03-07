/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 12:03 PM
 * <p>
 */

package com.dtech.admin.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;


@Configuration
public class MessageConfig {
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames("classpath:messages/response");
        messageSource.setCacheSeconds(10);
        return messageSource;
    }
}
