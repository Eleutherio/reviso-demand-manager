package com.guilherme.reviso_demand_manager.infra;

import com.guilherme.reviso_demand_manager.domain.StripeWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, String> {
    
    @Query(value = "INSERT INTO stripe_webhook_events (event_id, event_type, processed_at) " +
                   "VALUES (:eventId, :eventType, :processedAt) " +
                   "ON CONFLICT (event_id) DO NOTHING RETURNING event_id", nativeQuery = true)
    String insertIfNotExists(@Param("eventId") String eventId, 
                            @Param("eventType") String eventType, 
                            @Param("processedAt") java.time.OffsetDateTime processedAt);
}
