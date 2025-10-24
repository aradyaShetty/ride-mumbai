package com.ridemumbai.service;

import com.ridemumbai.model.*; // Import all models
import com.ridemumbai.repository.*; // Import all repositories
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Important for managing multiple DB operations

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID; // For generating unique IDs or QR data

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final TravelHistoryRepository travelHistoryRepository;
    private final RouteRepository routeRepository; // Needed to get route info
    private final UserRepository userRepository; // Needed to get current user
    private final RoutePlannerService routePlannerService; // Needed for fare calculation

    // --- Core Ticket Booking Logic (Placeholder) ---
    @Transactional // Ensures all DB operations succeed or fail together
    public Optional<Ticket> bookTicket(Long routeId, String ticketType, String paymentMethod) {
        log.info("Attempting to book ticket for routeId: {}, type: {}", routeId, ticketType);

        // 1. Get Authenticated User (Commuter)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        // Ensure the user is a Commuter (or handle appropriately if Admins can book)
        if (!(currentUser instanceof Commuter)) {
            log.error("User {} is not a Commuter, cannot book ticket.", currentUsername);
            return Optional.empty(); // Or throw specific exception
        }
        Commuter commuter = (Commuter) currentUser;
        Long commuterId = commuter.getUserId();

        // 2. Validate Route
        Optional<Route> routeOpt = routeRepository.findById(routeId);
        if (routeOpt.isEmpty()) {
            log.error("Invalid routeId {} provided for booking.", routeId);
            return Optional.empty(); // Or throw exception
        }
        Route route = routeOpt.get();

        // 3. Calculate Fare (using RoutePlannerService)
        Double fare = routePlannerService.calculateFare(route);
        log.info("Calculated fare: {}", fare);

        // 4. Process Payment (Simplified Placeholder)
        // TODO: Integrate with actual payment gateway (e.g., Razorpay, Stripe)
        // This involves redirecting user, handling callbacks, verifying payment status
        boolean paymentSuccess = processMockPayment(commuter, fare, paymentMethod);

        if (!paymentSuccess) {
            log.error("Payment failed for user {}, amount {}", commuterId, fare);
            // Need to save payment attempt with FAILED status
            savePaymentRecord(null, fare, paymentMethod, "FAILED", "MOCK_FAIL_" + UUID.randomUUID());
            return Optional.empty(); // Or throw PaymentFailedException
        }
        log.info("Mock payment successful.");

        // 5. Create Ticket
        String qrCodeData = generateQRCodeData(commuterId, routeId, LocalDateTime.now());
        Ticket newTicket = Ticket.builder()
                .commuterId(commuterId)
                .routeId(routeId)
                .fare(fare)
                .bookingDateTime(LocalDateTime.now())
                .ticketType(ticketType)
                .qrCodeData(qrCodeData)
                .status("BOOKED")
                .build();
        Ticket savedTicket = ticketRepository.save(newTicket);
        log.info("Ticket created with ID: {}", savedTicket.getTicketId());

        // 6. Record Successful Payment
        Payment paymentRecord = savePaymentRecord(savedTicket.getTicketId(), fare, paymentMethod, "SUCCESS",
                "MOCK_TXN_" + UUID.randomUUID());
        log.info("Payment recorded with ID: {}", paymentRecord.getPaymentId());

        // 7. Add to Travel History (Optional - might happen when ticket is USED)
        // For simplicity, let's add it now, assuming booking = start of potential
        // travel
        addJourneyToHistory(commuterId, savedTicket, route);
        log.info("Journey added to travel history for user {}", commuterId);

        // 8. Return the created ticket
        return Optional.of(savedTicket);
    }

    // --- Helper Methods (Placeholders / Simplified Logic) ---

    private boolean processMockPayment(Commuter commuter, Double amount, String method) {
        // Simulate payment processing
        log.info("Processing mock payment of {} via {} for user {}", amount, method, commuter.getUserId());
        // In reality: call payment gateway API, handle response
        // For Wallet: Check commuter.getWalletBalance(), deduct if sufficient
        if ("WALLET".equalsIgnoreCase(method)) {
            if (commuter.getWalletBalance() >= amount) {
                commuter.setWalletBalance(commuter.getWalletBalance() - amount);
                userRepository.save(commuter); // Update wallet balance in DB
                log.info("Deducted {} from wallet. New balance: {}", amount, commuter.getWalletBalance());
                return true;
            } else {
                log.warn("Insufficient wallet balance ({}) for payment ({})", commuter.getWalletBalance(), amount);
                return false;
            }
        }
        // Simulate success for other methods for now
        return true;
    }

    private Payment savePaymentRecord(Long ticketId, Double amount, String method, String status,
            String transactionId) {
        Payment payment = Payment.builder()
                .ticketId(ticketId) // Can be null if payment fails before ticket creation
                .amount(amount)
                .paymentDateTime(LocalDateTime.now())
                .paymentMethod(method)
                .status(status)
                .transactionId(transactionId)
                .build();
        return paymentRepository.save(payment);
    }

    private String generateQRCodeData(Long commuterId, Long routeId, LocalDateTime timestamp) {
        // Generate a unique string containing relevant info
        // TODO: Consider encrypting or using a more robust format
        return String.format("UID:%d-RID:%d-TS:%s-%s",
                commuterId, routeId, timestamp.toString(), UUID.randomUUID());
    }

    private void addJourneyToHistory(Long commuterId, Ticket ticket, Route route) {
        TravelHistory history = TravelHistory.builder()
                .commuterId(commuterId)
                .ticketId(ticket.getTicketId())
                .journeyDateTime(ticket.getBookingDateTime()) // Or set when ticket is actually used
                .startStationName(route.getStartStationName())
                .endStationName(route.getEndStationName())
                .farePaid(ticket.getFare())
                .build();
        travelHistoryRepository.save(history);
    }

    // TODO: Implement cancelTicket, validateQRCode etc. based on class diagram
    // methods
    public boolean cancelTicket(Long ticketId) {
        log.warn("cancelTicket not implemented yet.");
        return false;
    }

    // Fetches all tickets for a user (needed by CommuterController)
    public List<Ticket> getUserTickets(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return ticketRepository.findByCommuterIdOrderByBookingDateTimeDesc(user.getUserId());
    }

    // Implements "View Past Trips" (TravelHistory)
    public List<TravelHistory> getTravelHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return travelHistoryRepository.findByCommuterIdOrderByJourneyDateTimeDesc(user.getUserId());
    }

    // Implements "Cancel Ticket"
    @Transactional
    public boolean cancelTicket(Long ticketId, String username) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElse(null);

        if (ticket == null) {
            return false;
        }

        User user = userRepository.findByUsername(username)
                .orElse(null);
        
        // 1. Validate Ownership and Status
        if (user == null || !user.getUserId().equals(ticket.getCommuterId())) {
            return false;
        }

        if (!"BOOKED".equals(ticket.getStatus())) {
            return false;
        }

        // 2. Perform Refund and Payment Update
        Payment payment = paymentRepository.findByTicketId(ticketId).orElse(null);

        if (payment != null) {
            // Find Commuter to update balance
            Commuter commuter = (Commuter) userRepository.findById(user.getUserId()).get();
            
            // Simplified refund: If paid by wallet, add back the balance
            if ("WALLET".equalsIgnoreCase(payment.getPaymentMethod())) {
                commuter.setWalletBalance(commuter.getWalletBalance() + payment.getAmount());
                userRepository.save(commuter);
            }
            
            // Mark payment as refunded
            payment.setStatus("REFUNDED");
            paymentRepository.save(payment);
        }

        // 3. Mark ticket as cancelled
        ticket.setStatus("CANCELLED");
        ticketRepository.save(ticket);
        return true;
    }
}