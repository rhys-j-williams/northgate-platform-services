package com.meridian.platform.common;

import com.meridian.platform.common.correlation.CorrelationIdFilter;
import com.meridian.platform.common.correlation.CorrelationIdRestTemplateInterceptor;
import com.meridian.platform.common.error.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication
public class MeridianCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler meridianGlobalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public RestTemplateCustomizer correlationIdRestTemplateCustomizer() {
        return restTemplate -> restTemplate.getInterceptors().add(new CorrelationIdRestTemplateInterceptor());
    }
}
