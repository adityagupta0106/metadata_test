package com.serviceplus.metadata.aadhaarConfiguration.dto;

public class SaveAuaFieldMappingRequest {

    private Long fieldId;

    private String sourceType;

    private String sourcePath;

    private String transformation;

    private String defaultValue;

    private Boolean required;

    private Integer mappingOrder;

    private String responseAttributeType;
    private String desiredResponse;

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
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

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Integer getMappingOrder() {
        return mappingOrder;
    }

    public void setMappingOrder(Integer mappingOrder) {
        this.mappingOrder = mappingOrder;
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
}
