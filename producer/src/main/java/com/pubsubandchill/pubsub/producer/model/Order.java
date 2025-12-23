package com.pubsubandchill.pubsub.producer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @JsonProperty("order_id")
    private String orderId;
    
    @JsonProperty("customer_id")
    private String customerId;
    
    private List<String> items;
    
    @JsonProperty("total_amount")
    private Double totalAmount;
    
    private String status;
    
    private String timestamp;
    
    public Order(String orderId, String customerId, List<String> items, Double totalAmount) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = "pending";
        this.timestamp = Instant.now().toString();
    }
}

