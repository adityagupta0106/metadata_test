package com.serviceplus.metadata.aadhaarConfiguration.entity;


import jakarta.persistence.*;
import java.time.OffsetDateTime;

import static com.serviceplus.metadata.utility.ApplicationConstants.SP_SCHEMA_NAME;

@Entity
@Table(name = "aua_api_fields", schema = SP_SCHEMA_NAME)
public class AuaApiField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private AuaApiMessage message;

    @Column(name = "field_code", nullable = false)
    private String fieldCode;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "xpath", nullable = false)
    private String xpath;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    @Column(name = "field_type", nullable = false)
    private String fieldType;

    @Column(name = "required", nullable = false)
    private Boolean required = false;

    @Column(name = "multiple", nullable = false)
    private Boolean multiple = false;

    @Column(name = "sensitive", nullable = false)
    private Boolean sensitive = false;

    @Column(name = "displayable", nullable = false)
    private Boolean displayable = false;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "allowed_values", columnDefinition = "jsonb")
    private String allowedValues;

    @Column(name = "description")
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "generation_type")
    private String generationType;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "cr_date", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "up_date", nullable = false)
    private OffsetDateTime updatedAt;

    public AuaApiField() {
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

    public AuaApiMessage getMessage() {
        return message;
    }

    public void setMessage(AuaApiMessage message) {
        this.message = message;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Boolean getMultiple() {
        return multiple;
    }

    public void setMultiple(Boolean multiple) {
        this.multiple = multiple;
    }

    public Boolean getSensitive() {
        return sensitive;
    }

    public void setSensitive(Boolean sensitive) {
        this.sensitive = sensitive;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(String allowedValues) {
        this.allowedValues = allowedValues;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
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

    public Boolean getDisplayable() {
        return displayable;
    }

    public void setDisplayable(Boolean displayable) {
        this.displayable = displayable;
    }

    public String getGenerationType() {
        return generationType;
    }

    public void setGenerationType(String generationType) {
        this.generationType = generationType;
    }
}
