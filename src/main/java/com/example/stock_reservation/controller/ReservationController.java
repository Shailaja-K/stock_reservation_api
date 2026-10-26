
package com.example.stock_reservation.controller;

import com.example.stock_reservation.dto.ReservationResponse;
import com.example.stock_reservation.entity.Reservation;
import com.example.stock_reservation.service.ReservationService;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public Reservation createReservation(
            @RequestBody Map<Long, Integer> requestedItems) {
        return reservationService.createReservation(requestedItems);
    }

    @GetMapping("/{id}")
    public ReservationResponse getReservation(@PathVariable Long id) {
        return reservationService.getReservationDetails(id);
    }

    @PostMapping("/{id}/confirm")
    public Reservation confirmReservation(@PathVariable Long id) {
        return reservationService.confirmReservation(id);
    }

    @PostMapping("/{id}/cancel")
    public Reservation cancelReservation(@PathVariable Long id) {
        return reservationService.cancelReservation(id);
    }
}
