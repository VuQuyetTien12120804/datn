package com.bookingcare.backend_bookingcare.controller.admin;

import com.bookingcare.backend_bookingcare.common.ApiEnvelope;
import com.bookingcare.backend_bookingcare.service.AdminManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/slots")
@RequiredArgsConstructor
public class AdminSlotController {

    private final AdminManagementService adminManagementService;

    @PostMapping("/generate")
    public ApiEnvelope<List<Map<String, Object>>> generate(@RequestBody Map<String, Object> body) {
        return ApiEnvelope.ok(adminManagementService.generateSlots(body));
    }

    @GetMapping
    public ApiEnvelope<List<Map<String, Object>>> list(
            @RequestParam int doctorId,
            @RequestParam String date
    ) {
        return ApiEnvelope.ok(adminManagementService.listSlots(doctorId, date));
    }

    @PatchMapping("/{slotId}/deactivate")
    public ApiEnvelope<Map<String, Object>> deactivate(@PathVariable int slotId) {
        return ApiEnvelope.ok(adminManagementService.deactivateSlot(slotId));
    }
}
