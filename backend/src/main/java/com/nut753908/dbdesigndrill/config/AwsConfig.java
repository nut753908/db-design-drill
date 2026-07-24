package com.nut753908.dbdesigndrill.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

@Configuration
public class AwsConfig {

    @Bean
    public LambdaClient lambdaClient(@Value("${app.aws.region}") String region) {
        return LambdaClient.builder()
                .region(Region.of(region))
                .build();
    }
}
