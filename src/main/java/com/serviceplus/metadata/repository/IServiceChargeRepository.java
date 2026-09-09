package com.serviceplus.metadata.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.serviceplus.metadata.entity.ServiceChargeDetail;

public interface IServiceChargeRepository extends JpaRepository<ServiceChargeDetail, Object>{

	ServiceChargeDetail findByIdAndUserId(Long chargeDetailId, Long userId);

	ServiceChargeDetail findByIdAndServiceIdAndUserId(Long chargeDetailId, Integer serviceId, Long userId);

	List<ServiceChargeDetail> findByServiceIdAndUserId(Integer serviceId, Long userId);
	
	@Query(value = """
			SELECT EXISTS (
			    SELECT 1
			    FROM schm_sp.service_charge_detail
			    WHERE service_id = :serviceId
			      AND LOWER(charge_detail_json ->> 'templateName') = LOWER(:templateName)
			      AND (:id IS NULL OR id <> :id)
			)
			""", nativeQuery = true)
	boolean existsByServiceIdAndTemplateName(@Param("serviceId") Integer serviceId, @Param("templateName") String templateName, @Param("id") Long id);

}
