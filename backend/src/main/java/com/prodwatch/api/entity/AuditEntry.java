package com.prodwatch.api.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_entries")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Profile user;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "entity", nullable = false)
    private String entity;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "details")
    private String details;

    @Column(name = "entity_label")
    private String entityLabel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditEntry() {}

    public static AuditEntry create(
            Profile user, String action, String entity, UUID entityId, String details) {
        return create(user, action, entity, entityId, details, null);
    }

    public static AuditEntry create(
            Profile user, String action, String entity, UUID entityId, String details, String entityLabel) {
        AuditEntry entry = new AuditEntry();
        entry.setUser(user);
        entry.setAction(action);
        entry.setEntity(entity);
        entry.setEntityId(entityId);
        entry.setDetails(details);
        entry.setEntityLabel(entityLabel);
        return entry;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Profile getUser() {
        return user;
    }

    public void setUser(Profile user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getEntityLabel() {
        return entityLabel;
    }

    public void setEntityLabel(String entityLabel) {
        this.entityLabel = entityLabel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
