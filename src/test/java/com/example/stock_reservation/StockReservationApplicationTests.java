
package com.example.stock_reservation;

import com.example.stock_reservation.entity.Product;
import com.example.stock_reservation.entity.Reservation;
import com.example.stock_reservation.repository.ProductRepository;
import com.example.stock_reservation.service.ReservationService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class StockReservationApplicationTests {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReservationService reservationService;

    @Test
    void creatingReservationUpdatesReservedStock() {
        Product product = new Product();
        product.setSku("TEST-LAPTOP");
        product.setName("Test Laptop");
        product.setTotalStock(10);
        product.setReservedStock(0);

        product = productRepository.saveAndFlush(product);

        Reservation reservation = reservationService.createReservation(
                Map.of(product.getId(), 3));

        Product updatedProduct =
                productRepository.findById(product.getId()).orElseThrow();

        assertEquals("ACTIVE", reservation.getStatus());
        assertEquals(3, updatedProduct.getReservedStock());
        assertEquals(10, updatedProduct.getTotalStock());
    }
	
@Test
void reservationFailsWhenStockIsInsufficient() {
    Product product = new Product();
    product.setSku("TEST-INSUFFICIENT");
    product.setName("Test Product");
    product.setTotalStock(5);
    product.setReservedStock(0);

    product = productRepository.saveAndFlush(product);

    Long productId = product.getId();

    org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> reservationService.createReservation(
                    Map.of(productId, 6))
    );

    Product unchangedProduct =
            productRepository.findById(productId).orElseThrow();

    assertEquals(0, unchangedProduct.getReservedStock());
    assertEquals(5, unchangedProduct.getTotalStock());
}
	
@Test
void confirmingReservationUpdatesStock() {
    Product product = new Product();
    product.setSku("TEST-CONFIRM");
    product.setName("Test Confirm Product");
    product.setTotalStock(10);
    product.setReservedStock(0);

    product = productRepository.saveAndFlush(product);

    Reservation reservation = reservationService.createReservation(
            Map.of(product.getId(), 3));

    reservationService.confirmReservation(reservation.getId());

    Product updatedProduct =
            productRepository.findById(product.getId()).orElseThrow();

    assertEquals("CONFIRMED",
            reservationService.getReservation(reservation.getId()).getStatus());
    assertEquals(7, updatedProduct.getTotalStock());
    assertEquals(0, updatedProduct.getReservedStock());
}

@Test
void cancellingReservationReleasesReservedStock() {
    Product product = new Product();
    product.setSku("TEST-CANCEL");
    product.setName("Test Cancel Product");
    product.setTotalStock(10);
    product.setReservedStock(0);

    product = productRepository.saveAndFlush(product);

    Reservation reservation = reservationService.createReservation(
            Map.of(product.getId(), 3));

    reservationService.cancelReservation(reservation.getId());

    Product updatedProduct =
            productRepository.findById(product.getId()).orElseThrow();

    assertEquals("CANCELLED",
            reservationService.getReservation(reservation.getId()).getStatus());
    assertEquals(0, updatedProduct.getReservedStock());
    assertEquals(10, updatedProduct.getTotalStock());
}

@Test
void cancellingReservationTwiceDoesNotReleaseStockTwice() {
    Product product = new Product();
    product.setSku("TEST-CANCEL-TWICE");
    product.setName("Test Cancel Twice");
    product.setTotalStock(10);
    product.setReservedStock(0);

    product = productRepository.saveAndFlush(product);

    Reservation reservation = reservationService.createReservation(
            Map.of(product.getId(), 3));

    reservationService.cancelReservation(reservation.getId());

    org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> reservationService.cancelReservation(reservation.getId())
    );

    Product updatedProduct =
            productRepository.findById(product.getId()).orElseThrow();

    assertEquals(0, updatedProduct.getReservedStock());
    assertEquals(10, updatedProduct.getTotalStock());
    assertEquals("CANCELLED",
            reservationService.getReservation(reservation.getId()).getStatus());
}


@Test
void reservationFailsAtomicallyWhenOneProductHasInsufficientStock() {
    Product first = new Product();
    first.setSku("TEST-ATOMIC-A");
    first.setName("Test Atomic A");
    first.setTotalStock(10);
    first.setReservedStock(0);
    first = productRepository.saveAndFlush(first);

    Product second = new Product();
    second.setSku("TEST-ATOMIC-B");
    second.setName("Test Atomic B");
    second.setTotalStock(2);
    second.setReservedStock(0);
    second = productRepository.saveAndFlush(second);

    Long firstId = first.getId();
    Long secondId = second.getId();

    org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> reservationService.createReservation(
                    Map.of(firstId, 3, secondId, 5))
    );

    Product unchangedFirst =
            productRepository.findById(firstId).orElseThrow();
    Product unchangedSecond =
            productRepository.findById(secondId).orElseThrow();

    assertEquals(0, unchangedFirst.getReservedStock());
    assertEquals(10, unchangedFirst.getTotalStock());
    assertEquals(0, unchangedSecond.getReservedStock());
    assertEquals(2, unchangedSecond.getTotalStock());
}


@Test
void confirmingReservationTwiceDoesNotDeductStockTwice() {
    Product product = new Product();
    product.setSku("TEST-CONFIRM-TWICE");
    product.setName("Test Confirm Twice");
    product.setTotalStock(10);
    product.setReservedStock(0);

    product = productRepository.saveAndFlush(product);

    Reservation reservation = reservationService.createReservation(
            Map.of(product.getId(), 3));

    reservationService.confirmReservation(reservation.getId());

    org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> reservationService.confirmReservation(reservation.getId())
    );

    Product updatedProduct =
            productRepository.findById(product.getId()).orElseThrow();

    assertEquals(7, updatedProduct.getTotalStock());
    assertEquals(0, updatedProduct.getReservedStock());
    assertEquals("CONFIRMED",
            reservationService.getReservation(reservation.getId()).getStatus());
}


@Test
void reservationFailsWhenProductDoesNotExist() {
    org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> reservationService.createReservation(
                    Map.of(999999L, 1))
    );
}


@Test
void reservationFailsWhenQuantityIsZeroOrNegative() {
    Product product = new Product();
    product.setSku("TEST-INVALID-QTY");
    product.setName("Test Invalid Quantity");
    product.setTotalStock(10);
    product.setReservedStock(0);
    product = productRepository.saveAndFlush(product);

    Long productId = product.getId();

    org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> reservationService.createReservation(
                    Map.of(productId, 0))
    );

    org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> reservationService.createReservation(
                    Map.of(productId, -2))
    );

    Product unchanged =
            productRepository.findById(productId).orElseThrow();

    assertEquals(0, unchanged.getReservedStock());
    assertEquals(10, unchanged.getTotalStock());
}


@Test
void cancelledReservationCannotBeConfirmed() {
    Product product = new Product();
    product.setSku("TEST-CANCEL-CONFIRM");
    product.setName("Test Cancel Confirm");
    product.setTotalStock(10);
    product.setReservedStock(0);
    product = productRepository.saveAndFlush(product);

    Reservation reservation = reservationService.createReservation(
            Map.of(product.getId(), 3));

    reservationService.cancelReservation(reservation.getId());

    org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> reservationService.confirmReservation(reservation.getId())
    );

    Product updated =
            productRepository.findById(product.getId()).orElseThrow();

    assertEquals(10, updated.getTotalStock());
    assertEquals(0, updated.getReservedStock());
}

@Test
void confirmingNonexistentReservationFails() {
    org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> reservationService.confirmReservation(999999L)
    );
}


@Test
void reservationStoresMultipleProductsAndQuantities() {
    Product first = new Product();
    first.setSku("TEST-MULTI-A");
    first.setName("Test Multi A");
    first.setTotalStock(10);
    first.setReservedStock(0);
    first = productRepository.saveAndFlush(first);

    Product second = new Product();
    second.setSku("TEST-MULTI-B");
    second.setName("Test Multi B");
    second.setTotalStock(8);
    second.setReservedStock(0);
    second = productRepository.saveAndFlush(second);

    Reservation reservation = reservationService.createReservation(
            Map.of(first.getId(), 3, second.getId(), 2));

    var details = reservationService.getReservationDetails(
            reservation.getId());

    assertEquals("ACTIVE", details.getStatus());
    assertEquals(2, details.getItems().size());

    Product updatedFirst =
            productRepository.findById(first.getId()).orElseThrow();
    Product updatedSecond =
            productRepository.findById(second.getId()).orElseThrow();

    assertEquals(3, updatedFirst.getReservedStock());
    assertEquals(2, updatedSecond.getReservedStock());
}


}
