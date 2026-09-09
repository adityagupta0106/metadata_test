package com.serviceplus.metadata.aadhaarConfiguration.controller;

import com.serviceplus.metadata.aadhaarConfiguration.dto.AuaApiMappingResponse;
import com.serviceplus.metadata.aadhaarConfiguration.dto.AuaMastersResponse;
import com.serviceplus.metadata.aadhaarConfiguration.dto.SaveAuaApiMappingRequest;
import com.serviceplus.metadata.aadhaarConfiguration.service.AuaApiMappingService;
import com.serviceplus.metadata.exception.SPRuntimeError;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/a")
public class AuaApiMappingController {

    private final AuaApiMappingService mappingService;

    public AuaApiMappingController(AuaApiMappingService mappingService) {
        this.mappingService = mappingService;
    }

    @GetMapping("/aua/mapping")
    public ResponseEntity<List<AuaApiMappingResponse>> getApiMapping(@RequestParam Integer serviceId,@RequestParam Long auaId, HttpServletRequest request) {
        try {
            return ResponseEntity.ok(mappingService.getMapping(serviceId,auaId, request));
        } catch (Exception e) {
            if(e instanceof SPRuntimeError)
                throw e;

            e.printStackTrace();
            throw new SPRuntimeError("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/aua/mapping/save")
    public ResponseEntity<Void> saveMappings(@RequestParam Integer serviceId, @RequestParam Long apiId, @RequestBody SaveAuaApiMappingRequest data, HttpServletRequest request) {
        try {
            mappingService.saveMappings(apiId, data, request,serviceId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            if (e instanceof SPRuntimeError)
                throw e;

            e.printStackTrace();
            throw new SPRuntimeError("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}
