package com.northgate.platform.common;

import com.northgate.platform.common.correlation.CorrelationIdFilter;
import com.northgate.platform.common.correlation.CorrelationIdRestTemplateInterceptor;
import com.northgate.platform.common.error.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication
public class NorthgateCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler northgateGlobalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public RestTemplateCustomizer correlationIdRestTemplateCustomizer() {
        return restTemplate -> restTemplate.getInterceptors().add(new CorrelationIdRestTemplateInterceptor());
    }
}
