package com.pubsubandchill.pubsub.notification.service;

import com.pubsubandchill.pubsub.notification.model.Order;
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
public class NotificationService {

    @Value("${pubsub.project-id}")
    private String projectId;

    @Value("${pubsub.subscription-name}")
    private String subscriptionName;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MeterRegistry meterRegistry;
    private final Counter messagesProcessedCounter;
    private final Counter notificationErrorsCounter;
    private final Timer notificationTimeTimer;
    private final OpenTelemetry openTelemetry;
    private int messageCount = 0;
    private Subscriber subscriber;

    public NotificationService(OpenTelemetry openTelemetry, MeterRegistry meterRegistry) {
        this.openTelemetry = openTelemetry;
        this.meterRegistry = meterRegistry;
        this.messagesProcessedCounter = Counter.builder("pubsub.messages.processed")
                .description("Total number of messages processed")
                .tag("service", "notification")
                .register(meterRegistry);
        this.notificationErrorsCounter = Counter.builder("pubsub.messages.notification.errors")
                .description("Total number of errors while sending notifications")
                .tag("service", "notification")
                .register(meterRegistry);
        this.notificationTimeTimer = Timer.builder("pubsub.messages.notification.time")
                .description("Time taken to send notifications")
                .tag("service", "notification")
                .register(meterRegistry);
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

                // Send notification
                sendNotification(order);

                // Increment processed messages counter
                messagesProcessedCounter.increment();

                // Acknowledge the message
                consumer.ack();
                log.info("  ✓ Message acknowledged and deleted from subscription");
            } catch (Exception e) {
                // Increment error counter
                notificationErrorsCounter.increment();
                log.error("Error processing message", e);
                // Nack the message to retry later
                consumer.nack();
            } finally {
                sample.stop(notificationTimeTimer);
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

