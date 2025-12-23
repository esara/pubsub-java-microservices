package com.pubsubandchill.pubsub.producer.service;

import com.pubsubandchill.pubsub.producer.model.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class OrderProducerService {

    @Value("${pubsub.project-id}")
    private String projectId;

    @Value("${pubsub.topic-name}")
    private String topicName;

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private int orderCounter = 1;

    private final List<Map<String, Object>> orderTemplates = Arrays.asList(
        Map.of("customer_id", "CUST-001", "items", Arrays.asList("Laptop", "Mouse"), "total", 1200.00),
        Map.of("customer_id", "CUST-002", "items", Arrays.asList("Keyboard", "Monitor"), "total", 450.00),
        Map.of("customer_id", "CUST-001", "items", Arrays.asList("Headphones"), "total", 150.00),
        Map.of("customer_id", "CUST-003", "items", Arrays.asList("Webcam", "Microphone"), "total", 200.00),
        Map.of("customer_id", "CUST-002", "items", Arrays.asList("USB-C Hub"), "total", 75.00)
    );

    public OrderProducerService(PubSubTemplate pubSubTemplate) {
        this.pubSubTemplate = pubSubTemplate;
    }

    @Scheduled(fixedRate = 2000) // Publish every 2 seconds
    public void publishOrder() {
        try {
            // Select order template (cycling through them)
            Map<String, Object> orderTemplate = orderTemplates.get((orderCounter - 1) % orderTemplates.size());
            String orderId = String.format("ORD-%03d", orderCounter);

            Order order = new Order(
                orderId,
                (String) orderTemplate.get("customer_id"),
                (List<String>) orderTemplate.get("items"),
                ((Number) orderTemplate.get("total")).doubleValue()
            );

            // Convert order to JSON
            String messageJson = objectMapper.writeValueAsString(order);

            // Create Pub/Sub message attributes
            Map<String, String> attributes = new HashMap<>();
            attributes.put("order_type", "standard");
            attributes.put("priority", order.getTotalAmount() < 500 ? "normal" : "high");

            // Publish message using PubSubTemplate (automatically handles emulator)
            String messageId = pubSubTemplate.publish(topicName, messageJson, attributes).get();
            
            log.info("[{}] Published order {} - MessageId: {}", 
                Instant.now().toString(), orderId, messageId);
            
            orderCounter++;
        } catch (Exception e) {
            log.error("Error publishing message", e);
        }
    }
}

