package com.serviceplus.metadata.aadhaarConfiguration.entity;


import jakarta.persistence.*;
import java.time.OffsetDateTime;

import static com.serviceplus.metadata.utility.ApplicationConstants.SP_SCHEMA_NAME;

@Entity
@Table(name = "aua_api_field_mappings", schema = SP_SCHEMA_NAME)
public class AuaApiFieldMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "api_definition_id", nullable = false)
    private AuaApiDefinition apiDefinition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "api_field_id", nullable = false)
    private AuaApiField apiField;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "source_path")
    private String sourcePath;

    @Column(name = "transformation")
    private String transformation;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "response_attribute_type")
    private String responseAttributeType;

    @Column(name = "desired_response")
    private String desiredResponse;

    @Column(name = "service_id", nullable = false)
    private Integer serviceId;

    @Column(name = "cr_by")
    private Long createdBy;

    @Column(name = "up_by")
    private Long updatedBy;

    @Column(name = "cr_date", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "up_date", nullable = false)
    private OffsetDateTime updatedAt;

    public AuaApiFieldMapping() {
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

    public AuaApiField getApiField() {
        return apiField;
    }

    public void setApiField(AuaApiField apiField) {
        this.apiField = apiField;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getTransformation() {
        return transformation;
    }

    public void setTransformation(String transformation) {
        this.transformation = transformation;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
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

    public String getResponseAttributeType() {
        return responseAttributeType;
    }

    public void setResponseAttributeType(String responseAttributeType) {
        this.responseAttributeType = responseAttributeType;
    }

    public String getDesiredResponse() {
        return desiredResponse;
    }

    public void setDesiredResponse(String desiredResponse) {
        this.desiredResponse = desiredResponse;
    }

    public Integer getServiceId() {
        return serviceId;
    }

    public void setServiceId(Integer serviceId) {
        this.serviceId = serviceId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
