package com.serviceplus.metadata.aadhaarConfiguration.repository;

import com.serviceplus.metadata.aadhaarConfiguration.entity.AuaApiFieldMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuaApiFieldMappingRepository extends JpaRepository<AuaApiFieldMapping, Long> {

    Optional<AuaApiFieldMapping> findByApiDefinitionIdAndApiFieldIdAndTenantId(Long id, Long id1, String tenantId);

    Optional<AuaApiFieldMapping> findByServiceIdAndApiDefinitionIdAndApiFieldIdAndTenantId(Integer serviceId, Long apiId, Long id, String tenantId);

    List<AuaApiFieldMapping> findByServiceIdAndCreatedByAndTenantId(Integer serviceId, Long userId, String tenantId);
}
