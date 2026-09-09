package com.serviceplus.metadata.aadhaarConfiguration.repository;

import com.serviceplus.metadata.aadhaarConfiguration.entity.AuaApiField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuaApiFieldRepository extends JpaRepository<AuaApiField, Long> {

    List<AuaApiField> findByMessageIdAndActiveTrueAndTenantIdOrderByDisplayOrder(Long messageId, String tenantId);

    Optional<AuaApiField> findByIdAndActiveTrueAndTenantId(Long id, String tenantId);

    Optional<AuaApiField> findByMessageIdAndFieldCodeAndActiveTrueAndTenantId(Long messageId, String fieldCode, String tenantId);

    Optional<AuaApiField> findByIdAndMessageApiDefinitionIdAndActiveTrueAndTenantId(Long fieldId, Long apiDefinitionId, String tenantId);
}
