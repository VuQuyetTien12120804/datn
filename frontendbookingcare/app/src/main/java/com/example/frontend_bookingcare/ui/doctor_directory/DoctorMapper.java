package com.example.frontend_bookingcare.ui.doctor_directory;

import com.example.frontend_bookingcare.api.DoctorDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Backend trả về 1 dòng cho mỗi (doctor, specialty) qua LEFT JOIN. Cần gom
 * theo fullName để hiển thị 1 bác sĩ với nhiều chip chuyên khoa.
 */
public final class DoctorMapper {

    private DoctorMapper() {
    }

    public static List<DoctorDetail> groupByDoctor(List<DoctorDto> raw) {
        if (raw == null || raw.isEmpty()) return new ArrayList<>();

        Map<String, Accumulator> grouped = new LinkedHashMap<>();
        for (DoctorDto d : raw) {
            if (d == null) continue;
            String key = d.doctorId != null ? ("id:" + d.doctorId) : ("name:" + (d.fullName == null ? "" : d.fullName));
            Accumulator acc = grouped.get(key);
            if (acc == null) {
                acc = new Accumulator(d.doctorId, d.fullName, d.bio);
                grouped.put(key, acc);
            } else if ((acc.bio == null || acc.bio.isEmpty()) && d.bio != null && !d.bio.isEmpty()) {
                // bio có thể xuất hiện ở row bất kỳ do LEFT JOIN — giữ bio đầu tiên khác rỗng
                acc.bio = d.bio;
            }
            if (d.specialty != null && !d.specialty.trim().isEmpty() && !acc.specialties.contains(d.specialty.trim())) {
                acc.specialties.add(d.specialty.trim());
            }
        }

        List<DoctorDetail> result = new ArrayList<>();
        int i = 0;
        for (Accumulator acc : grouped.values()) {
            String[] tn = DoctorDetail.splitTitleAndName(acc.fullName);
            result.add(new DoctorDetail(
                    tn[0],
                    tn[1],
                    null,
                    acc.specialties,
                    null,
                    DoctorDetail.avatarBgForIndex(i),
                    acc.doctorId,
                    acc.bio
            ));
            i++;
        }
        return result;
    }

    private static final class Accumulator {
        final Integer doctorId;
        final String fullName;
        String bio;
        final List<String> specialties = new ArrayList<>();

        Accumulator(Integer doctorId, String fullName, String bio) {
            this.doctorId = doctorId;
            this.fullName = fullName;
            this.bio = bio;
        }
    }
}
