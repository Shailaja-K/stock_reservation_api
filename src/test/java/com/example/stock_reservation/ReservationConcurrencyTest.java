
package com.example.stock_reservation;

import com.example.stock_reservation.entity.Product;
import com.example.stock_reservation.repository.ProductRepository;
import com.example.stock_reservation.service.ReservationService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ReservationConcurrencyTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReservationService reservationService;

    @Test
    void concurrentReservationsMustNotOversellStock() throws Exception {

        Product product = new Product();
        product.setSku("CONCURRENT-" + UUID.randomUUID());
        product.setName("Concurrency Test Product");
        product.setTotalStock(5);
        product.setReservedStock(0);

        product = productRepository.saveAndFlush(product);
        Long productId = product.getId();

        CountDownLatch startSignal = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> firstRequest = executor.submit(() -> {
                startSignal.await();

                try {
                    reservationService.createReservation(
                            Map.of(productId, 4));
                    return true;
                } catch (IllegalArgumentException exception) {
                    return false;
                }
            });

            Future<Boolean> secondRequest = executor.submit(() -> {
                startSignal.await();

                try {
                    reservationService.createReservation(
                            Map.of(productId, 4));
                    return true;
                } catch (IllegalArgumentException exception) {
                    return false;
                }
            });

            // Release both requests at approximately the same time.
            startSignal.countDown();

            int successfulRequests =
                    (firstRequest.get() ? 1 : 0)
                    + (secondRequest.get() ? 1 : 0);

            Product updatedProduct =
                    productRepository.findById(productId).orElseThrow();

            assertEquals(1, successfulRequests,
                    "Exactly one reservation should succeed");

            assertEquals(5, updatedProduct.getTotalStock(),
                    "Total stock should remain unchanged");

            assertEquals(4, updatedProduct.getReservedStock(),
                    "Only four units should be reserved");
        } finally {
            executor.shutdownNow();
        }
    }
}
