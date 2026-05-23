package com.bookingcare.backend_bookingcare.service;

import com.bookingcare.backend_bookingcare.repository.AdminAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final AdminAnalyticsRepository analyticsRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> appointmentSummary(String from, String to) {
        LocalDate[] range = parseRange(from, to);
        return analyticsRepository.appointmentSummary(range[0], range[1]);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> timeseries(String from, String to, String granularity) {
        LocalDate[] range = parseRange(from, to);
        return analyticsRepository.timeseries(range[0], range[1], granularity);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> topDoctors(String from, String to, int limit) {
        LocalDate[] range = parseRange(from, to);
        return analyticsRepository.topDoctors(range[0], range[1], limit <= 0 ? 10 : limit);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> topSpecialties(String from, String to, int limit) {
        LocalDate[] range = parseRange(from, to);
        return analyticsRepository.topSpecialties(range[0], range[1], limit <= 0 ? 10 : limit);
    }

    private LocalDate[] parseRange(String from, String to) {
        LocalDate end = to != null && !to.isBlank() ? LocalDate.parse(to) : LocalDate.now();
        LocalDate start = from != null && !from.isBlank() ? LocalDate.parse(from) : end.minusDays(29);
        if (start.isAfter(end)) {
            start = end;
        }
        return new LocalDate[]{start, end};
    }
}
