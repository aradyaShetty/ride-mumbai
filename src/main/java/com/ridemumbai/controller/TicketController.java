package com.ridemumbai.controller;

import com.ridemumbai.auth.BookTicketRequest; // Adjust package if needed
import com.ridemumbai.model.Ticket;
import com.ridemumbai.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/book")
    public ResponseEntity<?> bookTicket(@RequestBody BookTicketRequest request) {
        Optional<Ticket> bookedTicket = ticketService.bookTicket(
                request.getRouteId(),
                request.getTicketType(),
                request.getPaymentMethod()
        );

        if (bookedTicket.isPresent()) {
            return ResponseEntity.ok(bookedTicket.get()); // Return the created Ticket entity
        } else {
            // Service layer should ideally throw specific exceptions
            // For now, return a generic error
            return ResponseEntity.badRequest().body("Ticket booking failed. Check route, payment, or user status.");
        }
    }

    // TODO: Add endpoints for getting user's tickets, cancelling tickets etc.
}