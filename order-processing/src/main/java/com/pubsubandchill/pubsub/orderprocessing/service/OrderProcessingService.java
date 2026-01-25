package com.pubsubandchill.pubsub.orderprocessing.service;

import com.pubsubandchill.pubsub.orderprocessing.model.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.OpenTelemetry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderProcessingService {

    @Value("${pubsub.project-id}")
    private String projectId;

    @Value("${pubsub.subscription-name}")
    private String subscriptionName;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MeterRegistry meterRegistry;
    private final Counter messagesProcessedCounter;
    private final Counter processingErrorsCounter;
    private final Timer processingTimeTimer;
    private final OpenTelemetry openTelemetry;
    private int messageCount = 0;
    private Subscriber subscriber;

    public OrderProcessingService(OpenTelemetry openTelemetry, MeterRegistry meterRegistry) {
        this.openTelemetry = openTelemetry;
        this.meterRegistry = meterRegistry;
        this.messagesProcessedCounter = Counter.builder("pubsub.messages.processed")
                .description("Total number of messages processed")
                .tag("service", "order-processing")
                .register(meterRegistry);
        this.processingErrorsCounter = Counter.builder("pubsub.messages.processing.errors")
                .description("Total number of errors while processing messages")
                .tag("service", "order-processing")
                .register(meterRegistry);
        this.processingTimeTimer = Timer.builder("pubsub.messages.processing.time")
                .description("Time taken to process messages")
                .tag("service", "order-processing")
                .register(meterRegistry);
    }

    @PostConstruct
    public void init() {
        log.info("=".repeat(60));
        log.info("Consumer Service 1: Order Processing Service");
        log.info("=".repeat(60));
        log.info("Project ID: {}", projectId);
        log.info("Subscription Name: {}", subscriptionName);
        log.info("-".repeat(60));
        log.info("Waiting for messages... (Press Ctrl+C to stop)");
        log.info("-".repeat(60));

        // Subscribe to messages
        subscribe();
    }

    private void subscribe() {
        ProjectSubscriptionName fullSubscriptionName =
                ProjectSubscriptionName.of(projectId, subscriptionName);
        MessageReceiver receiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            try {
                messageCount++;
                log.info("\n[{}] Received message #{}", Instant.now().toString(), messageCount);

                // Extract message payload
                String payload = message.getData().toStringUtf8();
                Order order = objectMapper.readValue(payload, Order.class);

                // Process the order
                processOrder(order);

                // Increment processed messages counter
                messagesProcessedCounter.increment();

                // Acknowledge the message
                consumer.ack();
                log.info("  ✓ Message acknowledged and deleted from subscription");
            } catch (Exception e) {
                // Increment error counter
                processingErrorsCounter.increment();
                log.error("Error processing message", e);
                // Nack the message to retry later
                consumer.nack();
            } finally {
                sample.stop(processingTimeTimer);
            }
        };

        subscriber = Subscriber.newBuilder(fullSubscriptionName, receiver)
                .setOpenTelemetry(openTelemetry)
                .setEnableOpenTelemetryTracing(true)
                .build();
        subscriber.startAsync().awaitRunning();
    }

    @PreDestroy
    public void shutdown() {
        if (subscriber == null) {
            return;
        }
        try {
            subscriber.stopAsync().awaitTerminated();
        } catch (Exception e) {
            log.warn("Failed to stop subscriber cleanly", e);
        }
    }

    private void processOrder(Order order) {
        try {
            log.info("  📦 Processing Order: {}", order.getOrderId());
            log.info("     Customer: {}", order.getCustomerId());
            log.info("     Items: {}", String.join(", ", order.getItems()));
            log.info("     Total: ${}", String.format("%.2f", order.getTotalAmount()));
            log.info("     Status: {} -> processing", order.getStatus());

            // Update order status
            order.setStatus("processing");
            order.setProcessedAt(Instant.now().toString());

            log.info("     ✅ Order {} is now being processed", order.getOrderId());
        } catch (Exception e) {
            log.error("  ❌ Error processing order: {}", e.getMessage());
            throw e;
        }
    }
}

