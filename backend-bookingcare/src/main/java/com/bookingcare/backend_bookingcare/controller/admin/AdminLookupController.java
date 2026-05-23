package com.bookingcare.backend_bookingcare.controller.admin;

import com.bookingcare.backend_bookingcare.common.ApiEnvelope;
import com.bookingcare.backend_bookingcare.service.AdminLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdminLookupController {

    private final AdminLookupService adminLookupService;

    /** Danh sách bệnh nhân (lookup). GET rooms/services dùng AdminRoomController / AdminServiceController. */
    @GetMapping("/api/v1/admin/patients")
    public ApiEnvelope<List<Map<String, Object>>> patients() {
        return ApiEnvelope.ok(adminLookupService.listPatients());
    }
}
