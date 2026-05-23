package com.example.frontend_bookingcare.ui.doctor_directory;

import com.example.frontend_bookingcare.api.DoctorDto;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Backend trả về 1 dòng cho mỗi (bác sĩ, chuyên khoa).
 * <ul>
 *   <li>Cùng {@code accountId} hoặc cùng tên + địa chỉ → một người (gộp chuyên khoa).</li>
 *   <li>Cùng {@code doctorId} qua nhiều dòng JOIN → gộp chuyên khoa (fallback theo ID).</li>
 * </ul>
 */
public final class DoctorMapper {

    private DoctorMapper() {
    }

    public static List<DoctorDetail> groupByDoctor(List<DoctorDto> raw) {
        if (raw == null || raw.isEmpty()) return new ArrayList<>();

        Map<String, Accumulator> grouped = new LinkedHashMap<>();
        for (DoctorDto d : raw) {
            if (d == null) continue;
            String key = groupKey(d);
            if (key.isEmpty()) continue;
            Accumulator acc = grouped.get(key);
            if (acc == null) {
                acc = new Accumulator(d.doctorId, d.fullName, d.bio, resolveAddress(d));
                grouped.put(key, acc);
            } else {
                acc.mergeDoctorId(d.doctorId);
                acc.mergeFullName(d.fullName);
                if ((acc.bio == null || acc.bio.isEmpty()) && d.bio != null && !d.bio.isEmpty()) {
                    acc.bio = d.bio;
                }
                if (acc.address == null || acc.address.isEmpty()) {
                    String addr = resolveAddress(d);
                    if (addr != null && !addr.isEmpty()) {
                        acc.address = addr;
                    }
                }
            }
            if (d.specialty != null && !d.specialty.trim().isEmpty()) {
                String spec = d.specialty.trim();
                if (!acc.specialties.contains(spec)) {
                    acc.specialties.add(spec);
                }
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
                    acc.address,
                    DoctorDetail.avatarBgForIndex(i),
                    acc.doctorId,
                    acc.bio
            ));
            i++;
        }
        return result;
    }

    /** accountId → tên+địa chỉ (trùng bản ghi DB) → doctorId → tên. */
    static String groupKey(DoctorDto d) {
        if (d.accountId != null) {
            return "acct:" + d.accountId;
        }
        String nameKey = normalizeDoctorName(d.fullName);
        String addrKey = normalizeAddress(resolveAddress(d));
        if (!nameKey.isEmpty() && !addrKey.isEmpty()) {
            return "person:" + nameKey + "|" + addrKey;
        }
        if (d.doctorId != null) {
            return "id:" + d.doctorId;
        }
        if (!nameKey.isEmpty()) {
            return "name:" + nameKey;
        }
        return "";
    }

    static String normalizeAddress(@Nullable String address) {
        if (address == null) return "";
        return address.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    static String normalizeDoctorName(String fullName) {
        if (fullName == null) return "";
        return fullName
                .replaceFirst("(?i)^(PGS\\.\\s*TS\\.\\s*BS\\.?|PGS\\.\\s*TS\\.?|TS\\.\\s*BS\\.?|ThS\\.\\s*BS\\.?|BS\\.\\s*CKII|BS\\.\\s*CKI|BS\\.|ThS\\.|TS\\.|PGS\\.|Dr\\.)\\s*", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private static String resolveAddress(DoctorDto d) {
        if (d.roomLocation != null && !d.roomLocation.trim().isEmpty()) {
            return d.roomLocation.trim();
        }
        if (d.clinicAddress != null && !d.clinicAddress.trim().isEmpty()) {
            return d.clinicAddress.trim();
        }
        return null;
    }

    private static final class Accumulator {
        Integer doctorId;
        String fullName;
        String bio;
        String address;
        final List<String> specialties = new ArrayList<>();

        Accumulator(Integer doctorId, String fullName, String bio, String address) {
            this.doctorId = doctorId;
            this.fullName = fullName != null ? fullName.trim() : "";
            this.bio = bio;
            this.address = address;
        }

        void mergeDoctorId(Integer otherId) {
            if (otherId == null) return;
            if (doctorId == null || otherId < doctorId) {
                doctorId = otherId;
            }
        }

        void mergeFullName(String other) {
            if (other == null || other.trim().isEmpty()) return;
            String trimmed = other.trim();
            if (fullName == null || fullName.isEmpty() || trimmed.length() > fullName.length()) {
                fullName = trimmed;
            }
        }
    }
}
