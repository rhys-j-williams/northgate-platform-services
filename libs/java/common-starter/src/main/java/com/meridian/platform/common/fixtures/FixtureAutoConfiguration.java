package com.meridian.platform.common.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ObjectMapper.class)
public class FixtureAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FixtureStore fixtureStore(@Value("${meridian.fixtures.path:}") String path) {
        return FixtureStore.load(path);
    }
}
