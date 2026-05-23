package com.bookingcare.backend_bookingcare.controller.admin;

import com.bookingcare.backend_bookingcare.common.ApiEnvelope;
import com.bookingcare.backend_bookingcare.common.PageResponse;

import java.util.List;
import java.util.Map;
import com.bookingcare.backend_bookingcare.service.AdminAppointmentService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/appointments")
@RequiredArgsConstructor
public class AdminAppointmentController {

    private final AdminAppointmentService adminAppointmentService;

    @GetMapping
    public ApiEnvelope<List<Map<String, Object>>> list(
            @RequestParam(required = false) Integer doctorId,
            @RequestParam(required = false) Integer patientId,
            @RequestParam(required = false) String status
    ) {
        return ApiEnvelope.ok(adminAppointmentService.list(doctorId, patientId, status));
    }

    @GetMapping("/search")
    public ApiEnvelope<PageResponse<Map<String, Object>>> search(
            @RequestParam(required = false) Integer doctorId,
            @RequestParam(required = false) Integer patientId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiEnvelope.ok(adminAppointmentService.search(
                doctorId, patientId, status, q, fromDate, toDate, page, size));
    }

    @GetMapping("/{id}/detail")
    public ApiEnvelope<Map<String, Object>> detail(@PathVariable int id) {
        return ApiEnvelope.ok(adminAppointmentService.detail(id));
    }

    @PatchMapping("/{id}/status")
    public ApiEnvelope<Map<String, Object>> updateStatus(
            @PathVariable int id,
            @RequestBody UpdateStatusRequest body
    ) {
        return ApiEnvelope.ok(adminAppointmentService.updateStatus(
                id, body.getStatus(), body.getNote(), body.getCancelReason()));
    }

    @PatchMapping("/{id}/reschedule")
    public ApiEnvelope<Map<String, Object>> reschedule(
            @PathVariable int id,
            @RequestParam int slotId
    ) {
        return ApiEnvelope.ok(adminAppointmentService.reschedule(id, slotId));
    }

    @Getter
    @Setter
    public static class UpdateStatusRequest {
        private String status;
        private String note;
        private String cancelReason;
    }
}
