package com.bookingcare.backend_bookingcare.controller;

import com.bookingcare.backend_bookingcare.common.ApiEnvelope;
import com.bookingcare.backend_bookingcare.dto.BookingRequestDto;
import com.bookingcare.backend_bookingcare.security.CurrentAccountService;
import com.bookingcare.backend_bookingcare.service.AppointmentQueueService;
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
    private final AppointmentQueueService appointmentQueueService;
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
        Map<String, Object> result = bookingService.book(accountId, body);
        // Tính queue sau khi transaction đặt lịch đã commit — khớp tab Lịch hẹn.
        Object id = result.get("appointmentId");
        if (id instanceof Number num) {
            Integer queueNo = appointmentQueueService.queueNumberFor(num.intValue());
            if (queueNo != null) {
                result.put("queueNumber", queueNo);
            }
        }
        return ApiEnvelope.ok(result);
    }
}
