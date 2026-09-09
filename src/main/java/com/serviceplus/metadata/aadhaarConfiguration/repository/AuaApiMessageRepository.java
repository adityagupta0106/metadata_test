package com.serviceplus.metadata.aadhaarConfiguration.repository;

import com.serviceplus.metadata.aadhaarConfiguration.entity.AuaApiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuaApiMessageRepository extends JpaRepository<AuaApiMessage, Long> {

    List<AuaApiMessage> findByApiDefinitionIdAndActiveTrueAndTenantIdOrderById(Long apiDefinitionId, String tenantId);

    Optional<AuaApiMessage> findByIdAndActiveTrueAndTenantId(Long id, String tenantId);

    Optional<AuaApiMessage> findByApiDefinitionIdAndMessageTypeAndActiveTrueAndTenantId(Long apiDefinitionId, String messageType, String tenantId);
}
