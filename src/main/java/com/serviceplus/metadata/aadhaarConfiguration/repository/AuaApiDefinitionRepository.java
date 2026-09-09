package com.serviceplus.metadata.aadhaarConfiguration.repository;


import com.serviceplus.metadata.aadhaarConfiguration.entity.AuaApiDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuaApiDefinitionRepository extends JpaRepository<AuaApiDefinition, Long> {

    List<AuaApiDefinition> findByProviderIdAndActiveTrueAndTenantId(Long providerId, String tenantId);

    Optional<AuaApiDefinition> findByIdAndActiveTrueAndTenantId(Long id, String tenantId);

    Optional<AuaApiDefinition> findByApiCodeAndActiveTrueAndTenantId(String apiCode, String tenantId);

    Optional<AuaApiDefinition> findByIdAndProviderIdAndActiveTrueAndTenantId(Long id, Long providerId, String tenantId);

    boolean existsByProviderIdAndApiCodeAndApiVersionAndTenantId(Long providerId, String apiCode, String apiVersion, String tenantId);

    List<AuaApiDefinition> findByProvider_IdAndActiveTrueAndTenantId(Long apiId, String tenantId);
}
