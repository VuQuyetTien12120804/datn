package com.bookingcare.backend_bookingcare.service;

import com.bookingcare.backend_bookingcare.repository.AppointmentRepository;
import com.bookingcare.backend_bookingcare.repository.AppointmentSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppointmentSlotService {

    static final Set<String> ACTIVE_BOOKING_STATUSES = Set.of("pending", "confirmed", "checked_in");

    private final AppointmentSlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public void syncBookedCount(Integer slotId) {
        if (slotId == null) {
            return;
        }
        slotRepository.findById(slotId).ifPresent(slot -> {
            long count = appointmentRepository.countBySlotIdAndStatusIn(slot.getId(), ACTIVE_BOOKING_STATUSES);
            slot.setBookedCount((int) count);
            slotRepository.save(slot);
        });
    }
}
