package com.guilherme.reviso_demand_manager.infra;

import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class ReportRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<RequestsByStatusRow> requestsByStatus(OffsetDateTime from, OffsetDateTime to, UUID agencyId) {
        String sql = """
            SELECT status, COUNT(*) AS total
            FROM request
            WHERE created_at >= :from AND created_at <= :to
              AND agency_id = :agencyId
            GROUP BY status
            ORDER BY total DESC
            """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("from", from);
        query.setParameter("to", to);
        query.setParameter("agencyId", agencyId);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        return results.stream()
            .map(row -> new RequestsByStatusRow((String) row[0], ((Number) row[1]).longValue()))
            .toList();
    }

    public OverdueRow overdue(OffsetDateTime at, UUID agencyId) {
        String sql = """
            SELECT COUNT(*) AS total
            FROM request
            WHERE due_date IS NOT NULL
              AND due_date < :at
              AND status NOT IN ('DELIVERED','CLOSED')
              AND agency_id = :agencyId
            """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("at", at);
        query.setParameter("agencyId", agencyId);
        Long total = ((Number) query.getSingleResult()).longValue();
        return new OverdueRow(total);
    }

    public AvgCycleTimeRow avgCycleTime(OffsetDateTime from, OffsetDateTime to, UUID agencyId) {
        String sql = """
            WITH delivered AS (
              SELECT re.request_id, MIN(re.created_at) AS delivered_at
              FROM request_event re
              JOIN request r ON r.id = re.request_id
              WHERE event_type = 'STATUS_CHANGED'
                AND status = 'DELIVERED'
                AND r.agency_id = :agencyId
              GROUP BY re.request_id
            )
            SELECT AVG(EXTRACT(EPOCH FROM (d.delivered_at - r.created_at))) AS avg_seconds
            FROM request r
            JOIN delivered d ON d.request_id = r.id
            WHERE r.created_at >= :from AND r.created_at <= :to
              AND r.agency_id = :agencyId
            """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("from", from);
        query.setParameter("to", to);
        query.setParameter("agencyId", agencyId);
        Object result = query.getSingleResult();
        Double avgSeconds = result != null ? ((Number) result).doubleValue() : 0.0;
        return new AvgCycleTimeRow(avgSeconds);
    }

    public ReworkMetricsRow reworkMetrics(OffsetDateTime from, OffsetDateTime to, UUID agencyId) {
        String sql = """
            WITH base AS (
              SELECT id
              FROM request
              WHERE created_at >= :from AND created_at <= :to
                AND agency_id = :agencyId
            ),
            rework AS (
              SELECT DISTINCT re.request_id
              FROM request_event re
              JOIN request r ON r.id = re.request_id
              WHERE re.event_type = 'STATUS_CHANGED'
                AND re.status = 'CHANGES_REQUESTED'
                AND re.created_at >= :from AND re.created_at <= :to
                AND r.agency_id = :agencyId
            )
            SELECT
              (SELECT COUNT(*) FROM rework) AS rework_count,
              (SELECT COUNT(*) FROM base) AS total_count
            """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("from", from);
        query.setParameter("to", to);
        query.setParameter("agencyId", agencyId);
        Object[] result = (Object[]) query.getSingleResult();
        Long reworkCount = result[0] != null ? ((Number) result[0]).longValue() : 0L;
        Long totalCount = result[1] != null ? ((Number) result[1]).longValue() : 0L;
        return new ReworkMetricsRow(reworkCount, totalCount);
    }

    public record RequestsByStatusRow(String status, Long total) {}
    public record OverdueRow(Long total) {}
    public record AvgCycleTimeRow(Double avgSeconds) {}
    public record ReworkMetricsRow(Long reworkCount, Long totalCount) {}
}

