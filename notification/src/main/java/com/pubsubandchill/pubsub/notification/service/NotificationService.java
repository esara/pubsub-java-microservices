package com.pubsubandchill.pubsub.notification.service;

import com.pubsubandchill.pubsub.notification.model.Order;
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
public class NotificationService {

    @Value("${pubsub.project-id}")
    private String projectId;

    @Value("${pubsub.subscription-name}")
    private String subscriptionName;

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private int messageCount = 0;

    public NotificationService(PubSubTemplate pubSubTemplate) {
        this.pubSubTemplate = pubSubTemplate;
    }

    @PostConstruct
    public void init() {
        log.info("=".repeat(60));
        log.info("Consumer Service 2: Notification Service");
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

                // Send notification
                sendNotification(order);

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

    private void sendNotification(Order order) {
        try {
            String customerId = order.getCustomerId();
            String orderId = order.getOrderId();
            Double total = order.getTotalAmount();

            log.info("  📧 Sending notification to customer {}", customerId);
            log.info("     Subject: Order Confirmation - {}", orderId);
            log.info("     Body: Your order for ${} has been received", String.format("%.2f", total));
            log.info("     Items: {}", String.join(", ", order.getItems()));

            // Simulate notification sending
            String notification = String.format(
                "To: %s@example.com\nSubject: Order Confirmation - %s\nBody: Thank you for your order! Order ID: %s, Total: $%.2f\nSent at: %s",
                customerId, orderId, orderId, total, Instant.now().toString()
            );

            log.info("     ✅ Notification sent successfully");
        } catch (Exception e) {
            log.error("  ❌ Error sending notification: {}", e.getMessage());
            throw e;
        }
    }
}

