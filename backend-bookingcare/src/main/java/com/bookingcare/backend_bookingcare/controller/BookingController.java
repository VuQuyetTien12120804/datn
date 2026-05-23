package com.bookingcare.backend_bookingcare.controller;

import com.bookingcare.backend_bookingcare.common.ApiEnvelope;
import com.bookingcare.backend_bookingcare.dto.BookingRequestDto;
import com.bookingcare.backend_bookingcare.security.CurrentAccountService;
import com.bookingcare.backend_bookingcare.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final CurrentAccountService currentAccountService;

    @GetMapping("/working-dates")
    public ApiEnvelope<List<String>> workingDates(@RequestParam int doctorId) {
        return ApiEnvelope.ok(bookingService.workingDates(doctorId));
    }

    @GetMapping("/slots")
    public ApiEnvelope<List<Map<String, Object>>> slots(
            @RequestParam int doctorId,
            @RequestParam String slotDate
    ) {
        return ApiEnvelope.ok(bookingService.slots(doctorId, slotDate));
    }

    @PostMapping("/book")
    public ApiEnvelope<Map<String, Object>> book(@Valid @RequestBody BookingRequestDto body) {
        int accountId = currentAccountService.requireUser().getAccountId();
        return ApiEnvelope.ok(bookingService.book(accountId, body));
    }
}
