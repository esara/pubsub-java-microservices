package com.pubsubandchill.pubsub.orderprocessing.service;

import com.pubsubandchill.pubsub.orderprocessing.model.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;

@Slf4j
@Service
public class OrderProcessingService {

    @Value("${pubsub.project-id}")
    private String projectId;

    @Value("${pubsub.subscription-name}")
    private String subscriptionName;

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private int messageCount = 0;

    public OrderProcessingService(PubSubTemplate pubSubTemplate) {
        this.pubSubTemplate = pubSubTemplate;
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
        pubSubTemplate.subscribe(subscriptionName, (message) -> {
            try {
                messageCount++;
                log.info("\n[{}] Received message #{}", Instant.now().toString(), messageCount);

                // Extract message payload
                String payload = message.getPubsubMessage().getData().toStringUtf8();
                Order order = objectMapper.readValue(payload, Order.class);

                // Process the order
                processOrder(order);

                // Acknowledge the message
                message.ack();
                log.info("  ✓ Message acknowledged and deleted from subscription");
            } catch (Exception e) {
                log.error("Error processing message", e);
                // Nack the message to retry later
                message.nack();
            }
        });
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

