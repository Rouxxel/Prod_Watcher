package com.prodwatch.api.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "workspace_settings")
public class WorkspaceSettings {

    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @Id
    private UUID id;

    @Column(name = "business_name", nullable = false)
    private String businessName = "";

    @Column(name = "contact_email", nullable = false)
    private String contactEmail = "";

    @Column(name = "tax_rate", nullable = false)
    private BigDecimal taxRate = new BigDecimal("0.16");

    @Column(name = "tax_label", nullable = false)
    private String taxLabel = "Tax";

    @Column(name = "receipt_footer")
    private String receiptFooter;

    @Column(name = "receipt_logo_url")
    private String receiptLogoUrl;

    @Column(name = "business_mode", nullable = false)
    private String businessMode = "auto";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Profile updatedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ecosystem_id", nullable = false)
    private Ecosystem ecosystem;

    protected WorkspaceSettings() {}

    public static WorkspaceSettings createDefault(Ecosystem ecosystem, BigDecimal taxRate) {
        WorkspaceSettings settings = new WorkspaceSettings();
        settings.id = UUID.randomUUID();
        settings.ecosystem = ecosystem;
        settings.businessName = "";
        settings.contactEmail = "";
        settings.taxRate = taxRate;
        settings.taxLabel = "Tax";
        settings.businessMode = "auto";
        settings.updatedAt = Instant.now();
        return settings;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public String getTaxLabel() {
        return taxLabel;
    }

    public void setTaxLabel(String taxLabel) {
        this.taxLabel = taxLabel;
    }

    public String getReceiptFooter() {
        return receiptFooter;
    }

    public void setReceiptFooter(String receiptFooter) {
        this.receiptFooter = receiptFooter;
    }

    public String getReceiptLogoUrl() {
        return receiptLogoUrl;
    }

    public void setReceiptLogoUrl(String receiptLogoUrl) {
        this.receiptLogoUrl = receiptLogoUrl;
    }

    public String getBusinessMode() {
        return businessMode;
    }

    public void setBusinessMode(String businessMode) {
        this.businessMode = businessMode;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Profile getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Profile updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Ecosystem getEcosystem() {
        return ecosystem;
    }

    public UUID getEcosystemId() {
        return ecosystem != null ? ecosystem.getId() : null;
    }
}
