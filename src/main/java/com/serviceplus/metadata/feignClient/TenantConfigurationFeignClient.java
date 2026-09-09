package com.serviceplus.metadata.feignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.serviceplus.metadata.dto.ConfigurationDTO;


@FeignClient(name = "InstanceConfigRegistry", path = "/configuration")
public interface TenantConfigurationFeignClient {
	final public static String HOST_HEADER = "SP-Client-Domain";
	
	@PostMapping("/a/find")
    ResponseEntity<ConfigurationDTO> getConfig(
            @RequestParam(required = false) String tenantId,
            @RequestHeader(value = HOST_HEADER, required = false) String host
    );
}
