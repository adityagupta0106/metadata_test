package com.serviceplus.metadata.aadhaarConfiguration.service;

import com.serviceplus.metadata.aadhaarConfiguration.dto.*;
import com.serviceplus.metadata.aadhaarConfiguration.entity.AuaApiDefinition;
import com.serviceplus.metadata.aadhaarConfiguration.entity.AuaApiField;
import com.serviceplus.metadata.aadhaarConfiguration.entity.AuaApiFieldMapping;
import com.serviceplus.metadata.aadhaarConfiguration.entity.AuaApiMessage;
import com.serviceplus.metadata.aadhaarConfiguration.model.AuaMappingSourceType;
import com.serviceplus.metadata.aadhaarConfiguration.model.AuaMappingTransformation;
import com.serviceplus.metadata.aadhaarConfiguration.model.AuaResponseAttributeType;
import com.serviceplus.metadata.aadhaarConfiguration.repository.AuaApiDefinitionRepository;
import com.serviceplus.metadata.aadhaarConfiguration.repository.AuaApiFieldMappingRepository;
import com.serviceplus.metadata.aadhaarConfiguration.repository.AuaApiFieldRepository;
import com.serviceplus.metadata.aadhaarConfiguration.repository.AuaApiMessageRepository;
import com.serviceplus.metadata.dto.UserSessionDTO;
import com.serviceplus.metadata.entity.ServiceDefinition;
import com.serviceplus.metadata.exception.SPRuntimeError;
import com.serviceplus.metadata.repository.IServiceDefinitionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.serviceplus.metadata.utility.CommonUtil.getUserSessionDetails;
import static com.serviceplus.metadata.utility.CommonUtil.isEmpty;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuaApiMappingService {

    private final AuaApiDefinitionRepository apiDefinitionRepository;
    private final AuaApiMessageRepository messageRepository;
    private final AuaApiFieldRepository fieldRepository;
    private final AuaApiFieldMappingRepository mappingRepository;
    private final IServiceDefinitionRepository serviceDefinitionRepository;

    public AuaApiMappingService(AuaApiDefinitionRepository apiDefinitionRepository, AuaApiMessageRepository messageRepository, AuaApiFieldRepository fieldRepository, AuaApiFieldMappingRepository mappingRepository, IServiceDefinitionRepository serviceDefinitionRepository) {
        this.apiDefinitionRepository = apiDefinitionRepository;
        this.messageRepository = messageRepository;
        this.fieldRepository = fieldRepository;
        this.mappingRepository = mappingRepository;
        this.serviceDefinitionRepository = serviceDefinitionRepository;
    }

    @Transactional(readOnly = true)
    public List<AuaApiMappingResponse> getMapping(Integer serviceId,Long providerId, HttpServletRequest request) {

        UserSessionDTO user = requireNonNull(getUserSessionDetails(request), "User session not found");

        String tenantId = user.getTenantId();

        ServiceDefinition service = serviceDefinitionRepository.findByServiceIdAndUserId(serviceId,user.getUserID());

        if(isNull(service)){
            throw new SPRuntimeError("You are not allowed to access this service", HttpStatus.UNAUTHORIZED);
        }

        if (providerId == null) {
            throw new SPRuntimeError("Provider ID is required", HttpStatus.BAD_REQUEST);
        }

        List<AuaApiDefinition> apis = apiDefinitionRepository.findByProvider_IdAndActiveTrueAndTenantId(providerId, tenantId);

        if (apis.isEmpty()) {
            throw new SPRuntimeError("No API definitions found for provider", HttpStatus.BAD_REQUEST);
        }

        List<AuaApiMappingResponse> responses = new ArrayList<>();

        for (AuaApiDefinition api : apis) {

            AuaApiMappingResponse response = new AuaApiMappingResponse();

            response.setApiId(api.getId());
            response.setApiCode(api.getApiCode());
            response.setApiName(api.getApiName());
            response.setOperationType(api.getOperationType());
            response.setApiVersion(api.getApiVersion());

            List<AuaApiMessage> messages = messageRepository.findByApiDefinitionIdAndActiveTrueAndTenantIdOrderById(api.getId(), tenantId);

            List<AuaApiMessageMappingResponse> messageResponses = new ArrayList<>();

            for (AuaApiMessage message : messages) {

                AuaApiMessageMappingResponse messageResponse = new AuaApiMessageMappingResponse();

                messageResponse.setMessageId(message.getId());
                messageResponse.setMessageType(message.getMessageType());
                messageResponse.setRootElement(message.getRootElement());

                List<AuaApiField> fields = fieldRepository.findByMessageIdAndActiveTrueAndTenantIdOrderByDisplayOrder(message.getId(), tenantId);

                List<AuaApiFieldMappingResponse> fieldResponses = new ArrayList<>();

                for (AuaApiField field : fields) {

                    AuaApiFieldMappingResponse fieldResponse = new AuaApiFieldMappingResponse();

                    fieldResponse.setFieldId(field.getId());
                    fieldResponse.setFieldCode(field.getFieldCode());
                    fieldResponse.setFieldName(field.getFieldName());
                    fieldResponse.setXpath(field.getXpath());
                    fieldResponse.setDataType(field.getDataType());
                    fieldResponse.setFieldType(field.getFieldType());
                    fieldResponse.setRequired(field.getRequired());
                    fieldResponse.setMultiple(field.getMultiple());
                    fieldResponse.setSensitive(field.getSensitive());
                    fieldResponse.setDisplay(field.getDisplayable());
                    fieldResponse.setTransformation(field.getGenerationType());

                    mappingRepository
                            .findByServiceIdAndApiDefinitionIdAndApiFieldIdAndTenantId(serviceId,api.getId(), field.getId(), tenantId)
                            .ifPresent(mapping -> {

                        MappingResponse mappingResponse = new MappingResponse();

                        mappingResponse.setMappingId(mapping.getId());
                        mappingResponse.setSourceType(mapping.getSourceType());
                        mappingResponse.setSourcePath(mapping.getSourcePath());
                        mappingResponse.setTransformation(mapping.getTransformation());
                        mappingResponse.setDefaultValue(mapping.getDefaultValue());
                        mappingResponse.setResponseAttributeType(mapping.getResponseAttributeType());
                        mappingResponse.setDesiredResponse(mapping.getDesiredResponse());

                        fieldResponse.setMapping(mappingResponse);
                    });

                    fieldResponses.add(fieldResponse);
                }

                messageResponse.setFields(fieldResponses);
                messageResponses.add(messageResponse);
            }

            response.setMessages(messageResponses);

            responses.add(response);
        }

        return responses;
    }


    @Transactional
    public void saveMappings(Long apiId, SaveAuaApiMappingRequest request, HttpServletRequest httpRequest,Integer serviceId) {

        UserSessionDTO user = requireNonNull(getUserSessionDetails(httpRequest), "User session not found");
        String tenantId = user.getTenantId();

        ServiceDefinition service = serviceDefinitionRepository.findByServiceIdAndUserId(serviceId,user.getUserID());

        if(isNull(service)){
            throw new SPRuntimeError("You are not allowed to access this service", HttpStatus.UNAUTHORIZED);
        }


        validateSaveRequest(apiId, request);

        AuaApiDefinition api = apiDefinitionRepository
                                    .findByIdAndActiveTrueAndTenantId(apiId, tenantId)
                                    .orElseThrow(() -> new RuntimeException("API definition not found"));

        /*
         * Validate that every frontend field belongs to
         * the selected API.
         */
        for (SaveAuaFieldMappingRequest item : request.getMappings()) {

            if (item.getFieldId() == null) {
                throw new SPRuntimeError("Field ID is required", HttpStatus.BAD_REQUEST);
            }

            AuaApiField field = fieldRepository
                                    .findByIdAndMessageApiDefinitionIdAndActiveTrueAndTenantId(item.getFieldId(), apiId, tenantId)
                                    .orElseThrow(() -> new SPRuntimeError("Field does not belong to selected API: " + item.getFieldId(), HttpStatus.BAD_REQUEST));

            validateMapping(item, field);
        }

        /*
         * Prevent duplicate field mappings in the same request.
         */
        Set<Long> fieldIds = new HashSet<>();

        for (SaveAuaFieldMappingRequest item : request.getMappings()) {

            if (!fieldIds.add(item.getFieldId())) {
                throw new SPRuntimeError("Duplicate mapping for field: " + item.getFieldId(), HttpStatus.BAD_REQUEST);
            }
        }

        /*
         * Save / update mappings.
         */
        OffsetDateTime now = OffsetDateTime.now();

        for (SaveAuaFieldMappingRequest item : request.getMappings()) {

            AuaApiField field = fieldRepository
                                    .findByIdAndMessageApiDefinitionIdAndActiveTrueAndTenantId(item.getFieldId(), apiId, tenantId)
                                    .orElseThrow(() -> new SPRuntimeError("Field does not belong to API", HttpStatus.BAD_REQUEST));

            AuaApiFieldMapping mapping = mappingRepository
                                            .findByServiceIdAndApiDefinitionIdAndApiFieldIdAndTenantId(serviceId,apiId, field.getId(), tenantId)
                                            .orElseGet(AuaApiFieldMapping::new);

            mapping.setTenantId(tenantId);

            mapping.setApiDefinition(api);
            mapping.setApiField(field);
            mapping.setSourceType(item.getSourceType());
            mapping.setSourcePath(item.getSourcePath());
            mapping.setTransformation(item.getTransformation());
            mapping.setDefaultValue(item.getDefaultValue());
            mapping.setResponseAttributeType(item.getResponseAttributeType());
            mapping.setDesiredResponse(item.getDesiredResponse());
            mapping.setServiceId(serviceId);
            mapping.setProviderId(api.getProvider().getId());

            if (mapping.getId() == null) {
                mapping.setCreatedAt(now);
                mapping.setCreatedBy(user.getUserID());
            }

            mapping.setUpdatedAt(now);
            mapping.setUpdatedBy(user.getUserID());

            mappingRepository.save(mapping);
        }
    }

    private void validateSaveRequest(Long apiId, SaveAuaApiMappingRequest request) {

        if (apiId == null) {
            throw new RuntimeException("API ID is required");
        }

        if (request == null) {
            throw new RuntimeException("Mapping request is required");
        }

        if (request.getMappings() == null || request.getMappings().isEmpty()) {
            throw new RuntimeException("At least one mapping is required");
        }
    }

    private void validateMapping(SaveAuaFieldMappingRequest item, AuaApiField field) {

        if (item.getFieldId() == null) {
            throw new SPRuntimeError("Field ID is required", HttpStatus.BAD_REQUEST);
        }

        if (!item.getFieldId().equals(field.getId())) {
            throw new SPRuntimeError("Invalid field ID", HttpStatus.BAD_REQUEST);
        }

        if (!isEmpty(item.getResponseAttributeType())) {

            try {

                AuaResponseAttributeType.valueOf(item.getResponseAttributeType().trim().toUpperCase());

            } catch (IllegalArgumentException ex) {
                throw new SPRuntimeError("Invalid response attribute type: " + item.getResponseAttributeType(), HttpStatus.BAD_REQUEST);
            }

            if (!isEmpty(item.getSourcePath())) {
                throw new SPRuntimeError("Source path must be empty for response mapping", HttpStatus.BAD_REQUEST);
            }

            if (!isEmpty(item.getDefaultValue())) {
                throw new SPRuntimeError("Default value must be empty for response mapping", HttpStatus.BAD_REQUEST);
            }

            return;
        }

        /*
         * ---------------------------------------------------------
         * REQUEST MAPPING
         * ---------------------------------------------------------
         */

        if (isEmpty(item.getSourceType())) {
            throw new SPRuntimeError("Source type is required for field: " + field.getFieldCode(), HttpStatus.BAD_REQUEST);
        }

        AuaMappingSourceType sourceType;

        try {

            sourceType = AuaMappingSourceType.valueOf(item.getSourceType().trim().toUpperCase());

        } catch (IllegalArgumentException ex) {

            throw new SPRuntimeError("Invalid source type: " + item.getSourceType() + " for field: " + field.getFieldCode(), HttpStatus.BAD_REQUEST);
        }

        /*
         * NONE should not be used for request mappings.
         */
        if (sourceType == AuaMappingSourceType.NONE) {
            throw new SPRuntimeError("NONE source type is not allowed for request field: " + field.getFieldCode(), HttpStatus.BAD_REQUEST);
        }

        if (sourceType == AuaMappingSourceType.CONSTANT || sourceType == AuaMappingSourceType.SECRET) {

            if (isEmpty(item.getDefaultValue())) {
                item.setDefaultValue("");
            }

            if (!isEmpty(item.getSourcePath())) {
                throw new SPRuntimeError("Source path must be empty for CONSTANT source", HttpStatus.BAD_REQUEST);
            }
        }

        /*
         * SECRET
         */
//        if (sourceType == AuaMappingSourceType.SECRET) {
//
//            if (isEmpty(item.getSourcePath())) {
//                throw new SPRuntimeError("Source path is required for SECRET source", HttpStatus.BAD_REQUEST);
//            }
//
//            if (!isEmpty(item.getDefaultValue())) {
//                throw new SPRuntimeError("Default value must be empty for SECRET source", HttpStatus.BAD_REQUEST);
//            }
//        }

        /*
         * DYNAMIC
         */
        if (sourceType == AuaMappingSourceType.DYNAMIC) {

            if (isEmpty(item.getSourcePath())) {

                throw new SPRuntimeError("Source path is required for DYNAMIC source", HttpStatus.BAD_REQUEST);
            }
        }

        /*
         * Response fields must not be sent with request mapping.
         */
        if (!isEmpty(item.getResponseAttributeType())) {
            throw new SPRuntimeError("Response attribute type must be empty for " + sourceType + " source", HttpStatus.BAD_REQUEST);
        }

        if (!isEmpty(item.getDesiredResponse())) {
            throw new SPRuntimeError("Desired response must be empty for " + sourceType + " source", HttpStatus.BAD_REQUEST);
        }

        if (isEmpty(field.getGenerationType())) {
            throw new SPRuntimeError("Generation type is not configured for field: " + field.getFieldCode(), HttpStatus.BAD_REQUEST);
        }

        try {

            AuaMappingTransformation.valueOf(field.getGenerationType().trim().toUpperCase());

        } catch (IllegalArgumentException ex) {

            throw new SPRuntimeError("Invalid generation type configured for field: " + field.getFieldCode() + ": " + field.getGenerationType(), HttpStatus.BAD_REQUEST);
        }
    }

}