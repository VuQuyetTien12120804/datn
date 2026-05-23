package com.bookingcare.backend_bookingcare.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AdminAnalyticsRepository {

    @PersistenceContext
    private EntityManager em;

    public Map<String, Object> appointmentSummary(LocalDate from, LocalDate to) {
        String sql = """
                SELECT
                  COUNT(*) AS total,
                  SUM(CASE WHEN a.status = N'completed' THEN 1 ELSE 0 END) AS completed,
                  SUM(CASE WHEN a.status = N'cancelled' THEN 1 ELSE 0 END) AS cancelled,
                  SUM(CASE WHEN a.status = N'no_show' THEN 1 ELSE 0 END) AS no_show,
                  SUM(CASE WHEN a.status = N'pending' THEN 1 ELSE 0 END) AS pending,
                  SUM(CASE WHEN a.status = N'confirmed' THEN 1 ELSE 0 END) AS confirmed
                FROM dbo.appointments a
                WHERE CAST(a.starts_at AS date) >= :fromDate
                  AND CAST(a.starts_at AS date) <= :toDate
                """;
        Query q = em.createNativeQuery(sql);
        q.setParameter("fromDate", from);
        q.setParameter("toDate", to);
        Object[] row = (Object[]) q.getSingleResult();
        Map<String, Object> m = new HashMap<>();
        m.put("total", ((Number) row[0]).longValue());
        m.put("completed", ((Number) row[1]).longValue());
        m.put("cancelled", ((Number) row[2]).longValue());
        m.put("noShow", ((Number) row[3]).longValue());
        m.put("pending", ((Number) row[4]).longValue());
        m.put("confirmed", ((Number) row[5]).longValue());
        return m;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> timeseries(LocalDate from, LocalDate to, String granularity) {
        String periodExpr = periodBucketExpression(granularity);
        String sql = """
                SELECT
                  %s AS period,
                  COUNT(*) AS total,
                  SUM(CASE WHEN a.status = N'confirmed' THEN 1 ELSE 0 END) AS confirmed,
                  SUM(CASE WHEN a.status = N'completed' THEN 1 ELSE 0 END) AS completed,
                  SUM(CASE WHEN a.status = N'cancelled' THEN 1 ELSE 0 END) AS cancelled,
                  SUM(CASE WHEN a.status = N'no_show' THEN 1 ELSE 0 END) AS no_show
                FROM dbo.appointments a
                WHERE CAST(a.starts_at AS date) >= :fromDate
                  AND CAST(a.starts_at AS date) <= :toDate
                GROUP BY %s
                ORDER BY period
                """.formatted(periodExpr, periodExpr);
        Query q = em.createNativeQuery(sql);
        q.setParameter("fromDate", from);
        q.setParameter("toDate", to);
        List<Object[]> rows = q.getResultList();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("period", r[0].toString());
            m.put("total", ((Number) r[1]).longValue());
            m.put("confirmed", ((Number) r[2]).longValue());
            m.put("completed", ((Number) r[3]).longValue());
            m.put("cancelled", ((Number) r[4]).longValue());
            m.put("noShow", ((Number) r[5]).longValue());
            out.add(m);
        }
        return out;
    }

    private static String periodBucketExpression(String granularity) {
        String g = granularity == null ? "day" : granularity.trim().toLowerCase();
        return switch (g) {
            case "week" -> """
                    CONVERT(varchar(10),
                      DATEADD(day, -((DATEPART(weekday, CAST(a.starts_at AS date)) + 5) %% 7),
                        CAST(a.starts_at AS date)), 23)""";
            case "month" -> "CONVERT(varchar(7), CAST(a.starts_at AS date), 126)";
            default -> "CONVERT(varchar(10), CAST(a.starts_at AS date), 23)";
        };
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> topDoctors(LocalDate from, LocalDate to, int limit) {
        String sql = """
                SELECT TOP (:lim)
                  d.id,
                  d.full_name,
                  COUNT(*) AS total,
                  SUM(CASE WHEN a.status = N'completed' THEN 1 ELSE 0 END) AS completed,
                  SUM(CASE WHEN a.status = N'cancelled' THEN 1 ELSE 0 END) AS cancelled,
                  SUM(CASE WHEN a.status = N'no_show' THEN 1 ELSE 0 END) AS no_show
                FROM dbo.appointments a
                INNER JOIN dbo.doctors d ON d.id = a.doctor_id
                WHERE CAST(a.starts_at AS date) >= :fromDate
                  AND CAST(a.starts_at AS date) <= :toDate
                GROUP BY d.id, d.full_name
                ORDER BY total DESC
                """;
        Query q = em.createNativeQuery(sql);
        q.setParameter("fromDate", from);
        q.setParameter("toDate", to);
        q.setParameter("lim", limit);
        return mapTopRows(q.getResultList());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> topSpecialties(LocalDate from, LocalDate to, int limit) {
        String sql = """
                SELECT TOP (:lim)
                  s.id,
                  s.name,
                  COUNT(*) AS total,
                  SUM(CASE WHEN a.status = N'completed' THEN 1 ELSE 0 END) AS completed,
                  SUM(CASE WHEN a.status = N'cancelled' THEN 1 ELSE 0 END) AS cancelled,
                  SUM(CASE WHEN a.status = N'no_show' THEN 1 ELSE 0 END) AS no_show
                FROM dbo.appointments a
                INNER JOIN dbo.doctors d ON d.id = a.doctor_id
                INNER JOIN dbo.doctor_specialties ds ON ds.doctor_id = d.id
                INNER JOIN dbo.specialties s ON s.id = ds.specialty_id
                WHERE CAST(a.starts_at AS date) >= :fromDate
                  AND CAST(a.starts_at AS date) <= :toDate
                GROUP BY s.id, s.name
                ORDER BY total DESC
                """;
        Query q = em.createNativeQuery(sql);
        q.setParameter("fromDate", from);
        q.setParameter("toDate", to);
        q.setParameter("lim", limit);
        return mapTopRows(q.getResultList());
    }

    private List<Map<String, Object>> mapTopRows(List<Object[]> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ((Number) r[0]).intValue());
            m.put("name", r[1].toString());
            m.put("total", ((Number) r[2]).longValue());
            m.put("completed", ((Number) r[3]).longValue());
            m.put("cancelled", ((Number) r[4]).longValue());
            m.put("noShow", ((Number) r[5]).longValue());
            out.add(m);
        }
        return out;
    }
}
