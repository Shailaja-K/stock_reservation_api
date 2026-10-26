
package com.example.stock_reservation.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ReservationResponse {

    private Long id;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ItemResponse> items;

    public ReservationResponse(
            Long id,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<ItemResponse> items) {
        this.id = id;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = items;
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<ItemResponse> getItems() {
        return items;
    }

    public static class ItemResponse {

        private Long productId;
        private String sku;
        private String name;
        private int quantity;

        public ItemResponse(
                Long productId, String sku, String name, int quantity) {
            this.productId = productId;
            this.sku = sku;
            this.name = name;
            this.quantity = quantity;
        }

        public Long getProductId() {
            return productId;
        }

        public String getSku() {
            return sku;
        }

        public String getName() {
            return name;
        }

        public int getQuantity() {
            return quantity;
        }
    }
}

