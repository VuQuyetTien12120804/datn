package com.bookingcare.backend_bookingcare.schedule;

import com.bookingcare.backend_bookingcare.service.AppointmentExpiryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentExpiryScheduler.class);

    private final AppointmentExpiryService appointmentExpiryService;

    /** Mỗi 30 phút quét hủy lịch pending quá hạn (VN). */
    @Scheduled(cron = "0 */30 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void expireStalePending() {
        int n = appointmentExpiryService.expireUnconfirmedPastPending();
        if (n > 0) {
            log.info("Auto-cancelled {} overdue pending appointment(s)", n);
        }
    }
}
