package com.bookingcare.backend_bookingcare.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DoctorPublicService {

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAll() {
        return queryDoctors(null);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listBySpecialty(int specialtyId) {
        return queryDoctors(specialtyId);
    }

    /**
     * Trả về một dòng cho mỗi (bác sĩ, chuyên khoa) để app Android gom theo doctorId.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> queryDoctors(Integer specialtyId) {
        String sql;
        if (specialtyId != null) {
            sql = """
                    SELECT d.id, d.account_id, d.full_name, s.name AS specialty, d.bio,
                           d.room_location,
                           (SELECT TOP 1 c.address FROM dbo.clinics c ORDER BY c.id) AS clinic_address
                    FROM dbo.doctors d
                    INNER JOIN dbo.doctor_specialties ds ON ds.doctor_id = d.id AND ds.specialty_id = :specId
                    INNER JOIN dbo.specialties s ON s.id = ds.specialty_id
                    ORDER BY d.full_name, s.name
                    """;
        } else {
            sql = """
                    SELECT d.id, d.account_id, d.full_name, s.name AS specialty, d.bio,
                           d.room_location,
                           (SELECT TOP 1 c.address FROM dbo.clinics c ORDER BY c.id) AS clinic_address
                    FROM dbo.doctors d
                    LEFT JOIN dbo.doctor_specialties ds ON ds.doctor_id = d.id
                    LEFT JOIN dbo.specialties s ON s.id = ds.specialty_id
                    ORDER BY d.full_name, s.name
                    """;
        }
        Query q = em.createNativeQuery(sql);
        if (specialtyId != null) {
            q.setParameter("specId", specialtyId);
        }
        List<Object[]> rows = q.getResultList();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("doctorId", ((Number) r[0]).intValue());
            m.put("accountId", r[1] != null ? ((Number) r[1]).intValue() : null);
            m.put("fullName", r[2].toString());
            m.put("specialty", r[3] != null ? r[3].toString() : null);
            m.put("bio", r[4] != null ? r[4].toString() : null);
            m.put("roomLocation", r[5] != null ? r[5].toString() : null);
            m.put("clinicAddress", r[6] != null ? r[6].toString() : null);
            out.add(m);
        }
        return out;
    }
}
