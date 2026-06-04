package com.bookingcare.backend_bookingcare.service;

import com.bookingcare.backend_bookingcare.common.AppointmentStatus;
import com.bookingcare.backend_bookingcare.entity.Appointment;
import com.bookingcare.backend_bookingcare.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Hủy tự động lịch pending đã qua khung giờ khám mà bác sĩ chưa xác nhận.
 */
@Service
@RequiredArgsConstructor
public class AppointmentExpiryService {

    public static final String AUTO_CANCEL_REASON =
            "Hệ thống hủy: bác sĩ không xác nhận trước giờ khám";

    private static final ZoneOffset VN = ZoneOffset.ofHours(7);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentSlotService appointmentSlotService;

    @Transactional
    public int expireUnconfirmedPastPending() {
        OffsetDateTime now = OffsetDateTime.now(VN);
        List<Appointment> stale = appointmentRepository.findByStatusAndEndsAtBefore(
                AppointmentStatus.PENDING, now);
        if (stale.isEmpty()) {
            return 0;
        }
        Set<Integer> slotIds = new HashSet<>();
        for (Appointment a : stale) {
            a.setStatus(AppointmentStatus.CANCELLED);
            a.setCancelReason(AUTO_CANCEL_REASON);
            a.setCancelledAt(now);
            a.setUpdatedAt(now);
            if (a.getSlotId() != null) {
                slotIds.add(a.getSlotId());
            }
        }
        appointmentRepository.saveAll(stale);
        for (Integer slotId : slotIds) {
            appointmentSlotService.syncBookedCount(slotId);
        }
        return stale.size();
    }
}
