package com.claims.mvp.configuration;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ServiceConfiguration {
    @Bean
    public ModelMapper getModelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        // Configure ModelMapper settings
        modelMapper.getConfiguration()
                // Enable matching by fields (not only by getters/setters)
                .setFieldMatchingEnabled(true)
                // Allow access to private fields of classes
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                // Use strict matching strategy — fields must match by name and type
                .setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper;
    }

}
