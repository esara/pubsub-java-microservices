package com.pubsubandchill.pubsub.orderprocessing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    
    @JsonProperty("processed_at")
    private String processedAt;
}

