
package com.example.stock_reservation.service;

import com.example.stock_reservation.dto.ReservationResponse;

import com.example.stock_reservation.entity.Product;
import com.example.stock_reservation.entity.Reservation;
import com.example.stock_reservation.entity.ReservationItem;
import com.example.stock_reservation.repository.ProductRepository;
import com.example.stock_reservation.repository.ReservationItemRepository;
import com.example.stock_reservation.repository.ReservationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ReservationService {

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationItemRepository reservationItemRepository;

    public ReservationService(
            ProductRepository productRepository,
            ReservationRepository reservationRepository,
            ReservationItemRepository reservationItemRepository) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
        this.reservationItemRepository = reservationItemRepository;
    }

    public Reservation getReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reservation not found: " + reservationId));
    }

    @Transactional
    public Reservation createReservation(Map<Long, Integer> requestedItems) {

        if (requestedItems == null || requestedItems.isEmpty()) {
            throw new IllegalArgumentException(
                    "Reservation must contain products");
        }

        // Sort product IDs to acquire product locks in a consistent order.
        Map<Long, Integer> sortedItems = new TreeMap<>(requestedItems);
        List<Product> products = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : sortedItems.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be positive");
            }

            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Product not found: " + productId));

            int availableStock =
                    product.getTotalStock() - product.getReservedStock();

            if (availableStock < quantity) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: " + product.getSku());
            }

            products.add(product);
        }

        Reservation reservation = new Reservation();
        reservation.setStatus("ACTIVE");
        reservationRepository.save(reservation);

        int index = 0;

        for (Map.Entry<Long, Integer> entry : sortedItems.entrySet()) {
            Product product = products.get(index++);
            int quantity = entry.getValue();

            product.setReservedStock(
                    product.getReservedStock() + quantity);
            productRepository.save(product);

            ReservationItem item = new ReservationItem(
                    reservation, product, quantity);
            reservationItemRepository.save(item);
        }

        return reservation;
    }

    @Transactional
    public Reservation confirmReservation(Long reservationId) {

        // Lock the reservation so it cannot be confirmed or cancelled
        // simultaneously by another transaction.
        Reservation reservation =
                reservationRepository.findByIdForUpdate(reservationId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Reservation not found: " + reservationId));

        if (!"ACTIVE".equals(reservation.getStatus())) {
           throw new IllegalStateException(
                "Only ACTIVE reservations can be confirmed");
        }

        List<ReservationItem> items =
                reservationItemRepository.findByReservationId(reservationId);

        // Acquire product locks in sorted order.
        Map<Long, ReservationItem> sortedItems = new TreeMap<>();
        for (ReservationItem item : items) {
            sortedItems.put(item.getProduct().getId(), item);
        }

        for (Map.Entry<Long, ReservationItem> entry : sortedItems.entrySet()) {
            ReservationItem item = entry.getValue();

            Product product = productRepository.findByIdForUpdate(
                            entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Product not found: " + entry.getKey()));

            int quantity = item.getQuantity();

            if (product.getReservedStock() < quantity
                    || product.getTotalStock() < quantity) {
                throw new IllegalStateException(
                        "Invalid stock quantities for product: "
                                + product.getSku());
            }

            product.setTotalStock(product.getTotalStock() - quantity);
            product.setReservedStock(product.getReservedStock() - quantity);

            productRepository.save(product);
        }

        reservation.setStatus("CONFIRMED");
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation cancelReservation(Long reservationId) {

        Reservation reservation =
                reservationRepository.findByIdForUpdate(reservationId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Reservation not found: " + reservationId));

        if (!"ACTIVE".equals(reservation.getStatus())) {
            throw new IllegalStateException(
               "Only ACTIVE reservations can be cancelled");
        }

        List<ReservationItem> items =
                reservationItemRepository.findByReservationId(reservationId);

        // Acquire product locks in sorted order.
        Map<Long, ReservationItem> sortedItems = new TreeMap<>();
        for (ReservationItem item : items) {
            sortedItems.put(item.getProduct().getId(), item);
        }

        for (Map.Entry<Long, ReservationItem> entry : sortedItems.entrySet()) {
            ReservationItem item = entry.getValue();

            Product product = productRepository.findByIdForUpdate(
                            entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Product not found: " + entry.getKey()));

            int quantity = item.getQuantity();

            if (product.getReservedStock() < quantity) {
                throw new IllegalStateException(
                        "Invalid reserved stock for product: "
                                + product.getSku());
            }

            product.setReservedStock(
                    product.getReservedStock() - quantity);

            productRepository.save(product);
        }

        reservation.setStatus("CANCELLED");
        return reservationRepository.save(reservation);
    }

    
@Transactional(readOnly = true)
public ReservationResponse getReservationDetails(Long reservationId) {

    Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException(
                    "Reservation not found: " + reservationId));

    List<ReservationItem> items =
            reservationItemRepository.findByReservationId(reservationId);

    List<ReservationResponse.ItemResponse> itemResponses =
            new ArrayList<>();

    for (ReservationItem item : items) {
        Product product = item.getProduct();

        itemResponses.add(new ReservationResponse.ItemResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                item.getQuantity()
        ));
    }

    return new ReservationResponse(
            reservation.getId(),
            reservation.getStatus(),
            reservation.getCreatedAt(),
            reservation.getUpdatedAt(),
            itemResponses
    );
}

}
