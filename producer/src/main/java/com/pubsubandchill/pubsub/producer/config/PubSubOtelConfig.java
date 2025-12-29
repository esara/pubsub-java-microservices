package com.pubsubandchill.pubsub.producer.config;

import com.google.cloud.spring.pubsub.core.publisher.PublisherCustomizer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PubSubOtelConfig {

    @Bean
    public OpenTelemetry openTelemetry() {
        return AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
    }

    @Bean
    public PublisherCustomizer pubSubPublisherOtelCustomizer(OpenTelemetry openTelemetry) {
        return (publisherBuilder, topic) -> publisherBuilder
                .setOpenTelemetry(openTelemetry)
                .setEnableOpenTelemetryTracing(true);
    }
}
