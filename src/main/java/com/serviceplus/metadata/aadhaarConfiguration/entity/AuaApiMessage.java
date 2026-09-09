package com.serviceplus.metadata.aadhaarConfiguration.entity;


import jakarta.persistence.*;

import java.time.OffsetDateTime;

import static com.serviceplus.metadata.utility.ApplicationConstants.SP_SCHEMA_NAME;

@Entity
@Table(name = "aua_api_messages", schema = SP_SCHEMA_NAME)
public class AuaApiMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "api_definition_id", nullable = false)
    private AuaApiDefinition apiDefinition;

    @Column(name = "message_type", nullable = false)
    private String messageType;

    @Column(name = "root_element", nullable = false)
    private String rootElement;

    @Column(name = "xsd_content")
    private String xsdContent;

    @Column(name = "xsd_version")
    private String xsdVersion;

    @Column(name = "xsd_hash")
    private String xsdHash;

    @Column(name = "description")
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "cr_date", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "up_date", nullable = false)
    private OffsetDateTime updatedAt;

    public AuaApiMessage() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public AuaApiDefinition getApiDefinition() {
        return apiDefinition;
    }

    public void setApiDefinition(AuaApiDefinition apiDefinition) {
        this.apiDefinition = apiDefinition;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getRootElement() {
        return rootElement;
    }

    public void setRootElement(String rootElement) {
        this.rootElement = rootElement;
    }

    public String getXsdContent() {
        return xsdContent;
    }

    public void setXsdContent(String xsdContent) {
        this.xsdContent = xsdContent;
    }

    public String getXsdVersion() {
        return xsdVersion;
    }

    public void setXsdVersion(String xsdVersion) {
        this.xsdVersion = xsdVersion;
    }

    public String getXsdHash() {
        return xsdHash;
    }

    public void setXsdHash(String xsdHash) {
        this.xsdHash = xsdHash;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
