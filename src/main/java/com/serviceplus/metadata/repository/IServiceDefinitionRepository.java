package com.serviceplus.metadata.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.serviceplus.metadata.entity.ServiceDefinition;

@Repository
public interface IServiceDefinitionRepository extends JpaRepository<ServiceDefinition, Object>{

	ServiceDefinition findByServiceIdAndUserId(Integer serviceId, Long userId);

	List<ServiceDefinition> findByUserId(Long userId);
	
	@Query("SELECT DISTINCT sd FROM ServiceLog sl JOIN ServiceDefinition sd ON sl.serviceId = sd.serviceId WHERE sl.tenantId = :tenantId AND sl.serviceStatus IN :statuses")
	List<ServiceDefinition> findActiveServicesAndStatus(@Param("tenantId") String tenantId, @Param("statuses") List<Integer> statuses);
	
	public Optional<List<ServiceDefinition>> findByBaseServiceId(Integer baseServiceId);
//    boolean existsByDefinitionJsonNameIgnoreCase(String serviceName);
//
//    boolean existsByDefinitionJsonNameIgnoreCaseAndServiceIdNot(String serviceName,Integer serviceId);
	
	boolean existsByServiceNameIgnoreCase(String serviceName);

	boolean existsByServiceAbbreviation(String serviceAbbreviation);

	boolean existsByServiceNameIgnoreCaseAndServiceIdNot(String serviceName, Integer serviceId);

	boolean existsByServiceAbbreviationAndServiceIdNot(String serviceAbbreviation, Integer serviceId);

	
}
