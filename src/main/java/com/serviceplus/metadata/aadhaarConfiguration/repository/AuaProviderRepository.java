package com.serviceplus.metadata.aadhaarConfiguration.repository;

import com.serviceplus.metadata.aadhaarConfiguration.entity.AuaProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AuaProviderRepository extends JpaRepository<AuaProvider, Long> {

    List<AuaProvider> findByActiveTrueAndTenantId(String tenantId);

    Optional<AuaProvider> findByIdAndActiveTrueAndTenantId(Long id, String tenantId);

    Optional<AuaProvider> findByProviderNameAndTenantId(String providerName, String tenantId);

    boolean existsByProviderNameAndActiveTrueAndTenantId(String providerName, String tenantId);

    List<AuaProvider> findByTenantIdAndActiveTrueOrderByProviderNameAsc(String tenantId);
}
