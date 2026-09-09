package com.serviceplus.metadata.aadhaarConfiguration.entity;


import jakarta.persistence.*;
import java.time.OffsetDateTime;

import static com.serviceplus.metadata.utility.ApplicationConstants.SP_SCHEMA_NAME;

@Entity
@Table(name = "aua_api_definitions", schema = SP_SCHEMA_NAME)
public class AuaApiDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private AuaProvider provider;

    @Column(name = "api_code", nullable = false)
    private String apiCode;

    @Column(name = "api_name", nullable = false)
    private String apiName;

    @Column(name = "operation_type", nullable = false)
    private String operationType;

    @Column(name = "api_version")
    private String apiVersion;

    @Column(name = "protocol", nullable = false)
    private String protocol = "HTTPS";

    @Column(name = "http_method", nullable = false)
    private String httpMethod = "POST";

    @Column(name = "endpoint")
    private String endpoint;

    @Column(name = "description")
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "xml_namespace")
    private String xmlNamespace;

    @Column(name = "xml_namespace_version")
    private String xmlNamespaceVersion;

    @Column(name = "cr_date", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "up_date", nullable = false)
    private OffsetDateTime updatedAt;

    public AuaApiDefinition() {
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

    public AuaProvider getProvider() {
        return provider;
    }

    public void setProvider(AuaProvider provider) {
        this.provider = provider;
    }

    public String getApiCode() {
        return apiCode;
    }

    public void setApiCode(String apiCode) {
        this.apiCode = apiCode;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
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

    public String getXmlNamespace() {
        return xmlNamespace;
    }

    public void setXmlNamespace(String xmlNamespace) {
        this.xmlNamespace = xmlNamespace;
    }

    public String getXmlNamespaceVersion() {
        return xmlNamespaceVersion;
    }

    public void setXmlNamespaceVersion(String xmlNamespaceVersion) {
        this.xmlNamespaceVersion = xmlNamespaceVersion;
    }
}
