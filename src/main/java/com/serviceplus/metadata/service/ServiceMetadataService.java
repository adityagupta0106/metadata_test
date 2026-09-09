package com.serviceplus.metadata.service;

import static com.serviceplus.metadata.utility.ApplicationConstants.GATEWAY_BEHAVIOUR_EXCLUSIVE_DIVERGENT;
import static com.serviceplus.metadata.utility.ApplicationConstants.GATEWAY_BEHAVIOUR_INCLUSIVE_DIVERGENT;
import static com.serviceplus.metadata.utility.ApplicationConstants.TYPE_GATEWAY;
import static com.serviceplus.metadata.utility.ApplicationConstants.TYPE_TASK;
import static com.serviceplus.metadata.utility.CommonUtil.createAttributeId;
import static com.serviceplus.metadata.utility.CommonUtil.entityToString;
import static com.serviceplus.metadata.utility.SnowflakeIdGenerator.createUniqueId;
import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.serviceplus.metadata.aadhaarConfiguration.entity.*;
import com.serviceplus.metadata.aadhaarConfiguration.model.AuaMappingSourceType;
import com.serviceplus.metadata.aadhaarConfiguration.model.AuaMappingTransformation;
import com.serviceplus.metadata.aadhaarConfiguration.repository.*;
import com.serviceplus.metadata.dto.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.serviceplus.metadata.document.mapping.dto.DocMappingDTO;
import com.serviceplus.metadata.document.mapping.dto.TaskDocs;
import com.serviceplus.metadata.dto.FileViewResponse.Data;
import com.serviceplus.metadata.dto.FormDTO.Attribute;
import com.serviceplus.metadata.dto.OfficeDetailsDTO.OfficeUnitData;
import com.serviceplus.metadata.dto.ProcessFlowDTO.AssociatedTaskDTO;
import com.serviceplus.metadata.dto.ProcessFlowDTO.DataDTO;
import com.serviceplus.metadata.dto.ProcessFlowDTO.DataDTO.Action;
import com.serviceplus.metadata.dto.ProcessFlowDTO.DataDTO.AssociatedTaskReferenceDTO;
import com.serviceplus.metadata.dto.ProcessFlowDTO.DataDTO.Escalation;
import com.serviceplus.metadata.dto.ProcessFlowDTO.EdgeDTO;
import com.serviceplus.metadata.dto.ProcessFlowDTO.Entities;
import com.serviceplus.metadata.dto.ProcessFlowDTO.ExternalSystemDetail;
import com.serviceplus.metadata.dto.ProcessFlowDTO.FormDetail;
import com.serviceplus.metadata.dto.ProcessFlowDTO.NodeDTO;
import com.serviceplus.metadata.dto.ProcessFlowDTO.Static;
import com.serviceplus.metadata.dto.ProcessFlowDTO.Timer;
import com.serviceplus.metadata.dto.ProcessFlowDTO.WebserviceDetails;
import com.serviceplus.metadata.dto.ServiceChargeDTO.PaymentModeDTO;
import com.serviceplus.metadata.dto.ServiceDeliveryUnitDTO.ServiceDeliveryUnitJSON;
import com.serviceplus.metadata.dto.ServiceParameterDetailsDTO.TaskDetails;
import com.serviceplus.metadata.dto.ServiceProcessFlowDTO.TaskRelationDTO;
import com.serviceplus.metadata.dto.TaskDTO.Task;
import com.serviceplus.metadata.dto.UserSessionDTO.Roles;
import com.serviceplus.metadata.dto.WorkFlowDataDTO.WorkFlowAction;
import com.serviceplus.metadata.entity.ServiceChargeDetail;
import com.serviceplus.metadata.entity.ServiceDefinition;
import com.serviceplus.metadata.entity.ServiceDeliveryUnitDefinition;
import com.serviceplus.metadata.entity.ServiceDeliveryUnits;
import com.serviceplus.metadata.entity.ServiceLog;
import com.serviceplus.metadata.entity.ServiceMasterData;
import com.serviceplus.metadata.entity.ServiceMetadata;
import com.serviceplus.metadata.entity.ServiceOutputFormat;
import com.serviceplus.metadata.entity.ServiceProcessFlow;
import com.serviceplus.metadata.entity.ServiceTemplate;
import com.serviceplus.metadata.entity.SpeSequenceLastNo;
import com.serviceplus.metadata.entity.SystemVariableMaster;
import com.serviceplus.metadata.entity.WorkflowAssignment;
import com.serviceplus.metadata.enums.ServiceStatus;
import com.serviceplus.metadata.enums.TaskType;
import com.serviceplus.metadata.exception.SPRuntimeError;
import com.serviceplus.metadata.feignClient.FileMgmtFeignClient;
import com.serviceplus.metadata.feignClient.FormDesignerFeignClient;
import com.serviceplus.metadata.feignClient.LgdFeignClient;
import com.serviceplus.metadata.formrecommendation.service.RecommendedFormKafkaProducer;
import com.serviceplus.metadata.inboxSentboxFilterConfig.dto.FilterJSONDTO;
import com.serviceplus.metadata.inboxSentboxFilterConfig.dto.FilterKafkaMessage;
import com.serviceplus.metadata.inboxSentboxFilterConfig.dto.FilterKafkaMessage.FilterKafkaDTO;
import com.serviceplus.metadata.inboxSentboxFilterConfig.dto.InboxSentBoxFilterDTO.FilterAttributeDTO;
import com.serviceplus.metadata.inboxSentboxFilterConfig.entity.InboxSentBoxFilter;
import com.serviceplus.metadata.inboxSentboxFilterConfig.enums.Status;
import com.serviceplus.metadata.inboxSentboxFilterConfig.repository.InboxSentBoxFilterRepository;
import com.serviceplus.metadata.kafka.KafkaProducer;
import com.serviceplus.metadata.mvel.dto.MvelFunctionDTO;
import com.serviceplus.metadata.mvel.dto.MvelFunctionDTO.TriggerPoint;
import com.serviceplus.metadata.mvel.dto.MvelFunctionEvent;
import com.serviceplus.metadata.mvel.entity.MvelFunction;
import com.serviceplus.metadata.mvel.repository.IMvelFunctionRepository;
import com.serviceplus.metadata.mvel.service.MvelKafkaProducer;
import com.serviceplus.metadata.notification.dto.NotificationConfigEvent;
import com.serviceplus.metadata.notification.dto.NotificationConfigEvent.NotificationProviderConfig;
import com.serviceplus.metadata.notification.dto.NotificationConfigEvent.NotificationTempleConfig;
import com.serviceplus.metadata.notification.dto.NotificationDTO.SandesConfig;
import com.serviceplus.metadata.notification.dto.NotificationDTO.SmsConfig;
import com.serviceplus.metadata.notification.dto.NotificationDTO.WhatsappConfig;
import com.serviceplus.metadata.notification.dto.SpecificAttributesDTO;
import com.serviceplus.metadata.notification.dto.SpecificAttributesReqDTO;
import com.serviceplus.metadata.notification.entity.NotificationConfiguration;
import com.serviceplus.metadata.notification.enums.NotificationType;
import com.serviceplus.metadata.notification.repository.INotificationRepository;
import com.serviceplus.metadata.notification.repository.NotificationProviderConfigRepository;
import com.serviceplus.metadata.notification.service.NotificationKafkaProducer;
import com.serviceplus.metadata.repository.IProcessFlowRepository;
import com.serviceplus.metadata.repository.IServiceChargeRepository;
import com.serviceplus.metadata.repository.IServiceDefinitionRepository;
import com.serviceplus.metadata.repository.IServiceDeliveryUnitDefinitionRepository;
import com.serviceplus.metadata.repository.IServiceLogRepository;
import com.serviceplus.metadata.repository.IServiceMasterDataRepository;
import com.serviceplus.metadata.repository.IServiceMetadataRepository;
import com.serviceplus.metadata.repository.IServiceOutputFormatRepository;
import com.serviceplus.metadata.repository.IServiceTemplateRepository;
import com.serviceplus.metadata.repository.ISpeSequenceLastNoRepository;
import com.serviceplus.metadata.repository.ISystemVariableRepository;
import com.serviceplus.metadata.repository.IWorkFlowAssignmentRepository;
import com.serviceplus.metadata.repository.ServiceDeliveryUnitsRepository;
import com.serviceplus.metadata.repository.ServiceDesignatedOfficerAssignmentRepository;
import com.serviceplus.metadata.utility.ApplicationConstants;

import jakarta.validation.Valid;

@Service
public class ServiceMetadataService {

    private static final Logger metadataCreationLogger = LogManager.getLogger("metadataCreationLogger");

    @Value("${kafka.topic.filter.configuration}")
    private String filterConfigurationTopic;
    
    @Value("${kafka.topic.notification-configuration}")
    private String notificationConfigurationTopic;
    private final IServiceDefinitionRepository serviceDefinitionRepository;
    private final IServiceLogRepository serviceLogRepository;
    private final ISpeSequenceLastNoRepository speSequenceLastNoRepo;
    
    private final IServiceTemplateRepository serviceTemplateRepository;
    private final IServiceOutputFormatRepository serviceOutputFormatRepository;
    private final IProcessFlowRepository processFlowRepository;
    private final LgdFeignClient lgdFeignClient;
    private final FormDesignerFeignClient formFeignClient;
    private final IServiceChargeRepository serviceChargeRepository;
    private final IServiceMasterDataRepository masterDataRepository;
    private final IServiceMetadataRepository metadataRepository;
    private final IMvelFunctionRepository mvelFunctionRepository;
    private final MvelKafkaProducer mvelKafkaProducer;
    private final ISystemVariableRepository systemVariableRepository;
    private final FileMgmtFeignClient fileMgmtFeignClient;
    private final ObjectMapper mapper;
    private final IWorkFlowAssignmentRepository workFlowAssignmentRepository;
    private final RecommendedFormKafkaProducer formKafkaProducer;
    private final NotificationKafkaProducer notificationKafkaProducer;
    private final IServiceDeliveryUnitDefinitionRepository serviceDeliveryUnitRepo;
    private final ServiceDesignatedOfficerAssignmentRepository serviceDesignatedOfficerAssignmentRepository;
    private final UserService userService;
    private final ServiceDeliveryUnitsRepository serviceDeliveryUnitsRepository;
    private final INotificationRepository notificationRepository;
    private final NotificationProviderConfigRepository notificationProviderConfigRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaProducer kafkaProducer;
    private final InboxSentBoxFilterRepository inboxSentBoxFilterRepository;
    private final AuaApiFieldMappingRepository mappingRepository;
    private final AuaApiDefinitionRepository apiDefinitionRepository;
    private final AuaApiMessageRepository messageRepository;
    private final AuaApiFieldRepository fieldRepository;
    private final AuaProviderRepository auaProviderRepository;

    public ServiceMetadataService(IServiceDefinitionRepository serviceDefinitionRepository, IServiceLogRepository serviceLogRepository, ISpeSequenceLastNoRepository speSequenceLastNoRepo, IServiceTemplateRepository serviceTemplateRepository, IServiceOutputFormatRepository serviceOutputFormatRepository, IProcessFlowRepository processFlowRepository, LgdFeignClient lgdFeignClient, FormDesignerFeignClient formFeignClient, IServiceChargeRepository serviceChargeRepository, IServiceMasterDataRepository masterDataRepository, IServiceMetadataRepository metadataRepository, IMvelFunctionRepository mvelFunctionRepository, MvelKafkaProducer mvelKafkaProducer, ISystemVariableRepository systemVariableRepository, FileMgmtFeignClient fileMgmtFeignClient, ObjectMapper mapper, IWorkFlowAssignmentRepository workFlowAssignmentRepository, RecommendedFormKafkaProducer formKafkaProducer, NotificationKafkaProducer notificationKafkaProducer, IServiceDeliveryUnitDefinitionRepository serviceDeliveryUnitRepo, ServiceDesignatedOfficerAssignmentRepository serviceDesignatedOfficerAssignmentRepository, UserService userService, ServiceDeliveryUnitsRepository serviceDeliveryUnitsRepository, INotificationRepository notificationRepository, NotificationProviderConfigRepository notificationProviderConfigRepository, RedisTemplate<String, String> redisTemplate, KafkaProducer kafkaProducer, InboxSentBoxFilterRepository inboxSentBoxFilterRepository, AuaApiFieldMappingRepository mappingRepository, AuaApiDefinitionRepository apiDefinitionRepository, AuaApiMessageRepository messageRepository, AuaApiFieldRepository fieldRepository, AuaProviderRepository auaProviderRepository) {
        this.serviceDefinitionRepository = serviceDefinitionRepository;
        this.serviceLogRepository = serviceLogRepository;
        this.speSequenceLastNoRepo = speSequenceLastNoRepo;
        this.serviceTemplateRepository = serviceTemplateRepository;
        this.serviceOutputFormatRepository = serviceOutputFormatRepository;
        this.processFlowRepository = processFlowRepository;
        this.lgdFeignClient = lgdFeignClient;
        this.formFeignClient = formFeignClient;
        this.serviceChargeRepository = serviceChargeRepository;
        this.masterDataRepository = masterDataRepository;
        this.metadataRepository = metadataRepository;
        this.mvelFunctionRepository = mvelFunctionRepository;
        this.mvelKafkaProducer = mvelKafkaProducer;
        this.systemVariableRepository = systemVariableRepository;
        this.fileMgmtFeignClient = fileMgmtFeignClient;
        this.mapper = mapper;
        this.workFlowAssignmentRepository = workFlowAssignmentRepository;
        this.formKafkaProducer = formKafkaProducer;
        this.notificationKafkaProducer = notificationKafkaProducer;
        this.serviceDeliveryUnitRepo = serviceDeliveryUnitRepo;
        this.serviceDesignatedOfficerAssignmentRepository = serviceDesignatedOfficerAssignmentRepository;
        this.userService = userService;
        this.serviceDeliveryUnitsRepository = serviceDeliveryUnitsRepository;
        this.notificationRepository = notificationRepository;
        this.notificationProviderConfigRepository = notificationProviderConfigRepository;
        this.redisTemplate = redisTemplate;
        this.kafkaProducer = kafkaProducer;
        this.inboxSentBoxFilterRepository = inboxSentBoxFilterRepository;
        this.mappingRepository = mappingRepository;
        this.apiDefinitionRepository = apiDefinitionRepository;
        this.messageRepository = messageRepository;
        this.fieldRepository = fieldRepository;
        this.auaProviderRepository = auaProviderRepository;
    }

    @Value("${file.upload-dir}")
    private String uploadDir;

	public ResponseEntity<?> getAllAllowActionOfTask(Integer serviceId, String taskId,
			UserSessionDTO userSessionDetails) {
		try {
			ServiceProcessFlow serviceProcessFlow = processFlowRepository.findByServiceIdAndUserId(serviceId,
					userSessionDetails.getUserID());
			if (serviceProcessFlow == null) {
				throw new SPRuntimeError("Service not found.", HttpStatus.BAD_REQUEST);
			}
			ProcessFlowDTO processFlowJson = serviceProcessFlow.getProcessFlowJson();
			if (processFlowJson == null) {
				throw new SPRuntimeError("Service Process Flow not found.", HttpStatus.BAD_REQUEST);
			}
			
			List<Map<String, String>> response = new ArrayList<>();
			for (NodeDTO node : processFlowJson.getNodes()) {
				if (taskId.equals(node.getId())) {
					DataDTO data = node.getData() != null ? node.getData() : new DataDTO();
					Map<String, Action> workflow = data.getWorkflow();
					if (workflow != null) {
						for (Map.Entry<String, Action> entry : workflow.entrySet()) {
							Action action = entry.getValue();
							if (Boolean.TRUE.equals(action.getEnabled())) {
								Map<String, String> actionMap = new HashMap<>();
								actionMap.put("label", action.getActionFormLabel());
								actionMap.put("value", entry.getKey());
								response.add(actionMap);
							}
						}
					}
					break;
				}
			}
			return ResponseEntity.ok(response);
		} catch (Exception ex) {
			ex.printStackTrace();
			if (ex instanceof SPRuntimeError) {
				throw ex;
			} else {
				metadataCreationLogger.error("Error : Get all allow action of task :", ex);
				throw new SPRuntimeError("Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
	}
    
    public ResponseEntity<?> getAllTaskOfAttachedForms(Integer serviceId, UserSessionDTO userSessionDetails) {
        try {
                ServiceProcessFlow serviceProcessFlow = processFlowRepository.findByServiceIdAndUserId(serviceId, userSessionDetails.getUserID());
                if(serviceProcessFlow  == null) {
                        throw new SPRuntimeError("Service not found.", HttpStatus.BAD_REQUEST);
                }
                ProcessFlowDTO processFlowJson = serviceProcessFlow.getProcessFlowJson();
                if(processFlowJson  == null) {
                        throw new SPRuntimeError("Service Process Flow not found.", HttpStatus.BAD_REQUEST);
                }
                List<Object> resData = new ArrayList<Object>();
                
                List<NodeDTO> nodes = processFlowJson.getNodes();
                for (NodeDTO node : nodes) {
                        DataDTO data = node.getData() != null ? node.getData() : new DataDTO();
                        FormDetail formDetail = data.getFormDetail();
                        if (formDetail != null) {
                                Map<String, Object> task = new HashMap<String, Object>();
                                task.put("taskId", node.getId());
                                task.put("taskName", data.getName());
                                task.put("taskType", node.getType());
                                task.put("formId", formDetail.getFormId());
                                task.put("formName", formDetail.getTemplateName());
                                task.put("holderId", formDetail.getHolderId());
                                resData.add(task);
                        }
                }
                return ResponseEntity.ok(resData);
        } catch (Exception ex) {
                ex.printStackTrace();
                if (ex instanceof SPRuntimeError) {
                        throw ex;
                } else {
                        metadataCreationLogger.error("Error : Get all get process flow tasks of attached forms :", ex);
                        throw new SPRuntimeError("Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
                }
        }
}

public ResponseEntity<?> getAllDouments(Integer serviceId, UserSessionDTO userSessionDetails) {
        try {
                ServiceProcessFlow serviceProcessFlow = processFlowRepository.findByServiceIdAndUserId(serviceId,
                                userSessionDetails.getUserID());
                if (serviceProcessFlow == null) {
                        throw new SPRuntimeError("Service not found.", HttpStatus.BAD_REQUEST);
                }
                ProcessFlowDTO processFlowJson = serviceProcessFlow.getProcessFlowJson();
                if (processFlowJson == null) {
                        throw new SPRuntimeError("Service Process Flow not found.", HttpStatus.BAD_REQUEST);
                }
                List<NodeDTO> nodes = processFlowJson.getNodes();
                List<TaskDocs> resData = new ArrayList<TaskDocs>();
                for (NodeDTO node : nodes) {
                        DataDTO data = node.getData();
                        if (data == null)
                                continue;
                        List<DocMappingDTO> documentMapping = data.getDocumentMapping();
                        TaskDocs tasksDoc = new TaskDocs();
                        tasksDoc.setTaskId(node.getId());
                        tasksDoc.setTaskName(data.getName());
                        List<TaskDocs.Document> documents = new ArrayList<TaskDocs.Document>();
                        if(documentMapping != null) {
                        	for (DocMappingDTO docMappingDTO : documentMapping) {
                        		TaskDocs.Document document = new TaskDocs.Document();
                        		document.setReferenceId(docMappingDTO.getReferenceId());
                        		document.setDocumentName(docMappingDTO.getDocumentName());
                        		documents.add(document);
                        	}
                        }
                        tasksDoc.setDocuments(documents);
                        resData.add(tasksDoc);
                }
                
                return ResponseEntity.ok(resData);
        } catch (Exception ex) {
                ex.printStackTrace();
                if (ex instanceof SPRuntimeError) {
                        throw ex;
                } else {
                        metadataCreationLogger.error("Error : Get all attached documents :", ex);
                        throw new SPRuntimeError("Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
                }
        }
}

public ResponseEntity<?> getProcessFlowFormsAttr(Integer serviceId, String taskId, String[] attributeTypes, UserSessionDTO userSessionDetails) {
	try {
		ServiceProcessFlow serviceProcessFlow = processFlowRepository.findByServiceIdAndUserId(serviceId,
				userSessionDetails.getUserID());

		if (serviceProcessFlow == null) {
			throw new SPRuntimeError("Service not found.", HttpStatus.BAD_REQUEST);
		}

		ProcessFlowDTO processFlowJson = serviceProcessFlow.getProcessFlowJson();
		if (processFlowJson == null) {
			throw new SPRuntimeError("Service Process Flow not found.", HttpStatus.BAD_REQUEST);
		}

		List<NodeDTO> nodes = processFlowJson.getNodes();
		Set<String> formIds = new HashSet<>();
		FormDetail formDetail = null;

		for (NodeDTO node : nodes) {
			if (taskId.equals(node.getId())) {
				DataDTO data = node.getData() != null ? node.getData() : new DataDTO();
				formDetail = data.getFormDetail();
				if (formDetail != null) {
					formIds.add(formDetail.getFormId());
				}
				break;
			}
		}

		if (formDetail == null) {
			throw new SPRuntimeError("Form not found.", HttpStatus.BAD_REQUEST);
		}

		SpecificAttributesReqDTO attributesReqDTO = new SpecificAttributesReqDTO();
		Map<String, SpecificAttributesDTO> specificAttributesDTOs = new HashMap<>();

		ServiceParameterDetailsDTO responseDTO = new ServiceParameterDetailsDTO();
		List<ServiceParameterDetailsDTO.TemplateDetails> templateList = new ArrayList<>();

		if (!formIds.isEmpty()) {

			ObjectMapper objectMapper = new ObjectMapper();

			for (String attributeType : attributeTypes) {

				attributesReqDTO.setAttrType(attributeType);
				attributesReqDTO.setFormIds(formIds);

				ResponseEntity<?> responseEntity = formFeignClient.getSpecificAttributes(attributesReqDTO);

				if (responseEntity != null && responseEntity.getStatusCode() == HttpStatus.OK) {

					Map<String, SpecificAttributesDTO> responseMap = objectMapper.convertValue(responseEntity.getBody(),
							new TypeReference<Map<String, SpecificAttributesDTO>>() {
							});

					for (Map.Entry<String, SpecificAttributesDTO> entry : responseMap.entrySet()) {

						String formId = entry.getKey();
						SpecificAttributesDTO dto = entry.getValue();

						if (specificAttributesDTOs.containsKey(formId)) {
							specificAttributesDTOs.get(formId).getAttributes().addAll(dto.getAttributes());
						} else {
							specificAttributesDTOs.put(formId, dto);
						}
					}
				}
			}

			String formId = formDetail.getFormId();
			String holderId = formDetail.getHolderId();

			SpecificAttributesDTO specificAttributesDTO = specificAttributesDTOs.get(formId);

			ServiceParameterDetailsDTO.TemplateDetails templateDTO = new ServiceParameterDetailsDTO.TemplateDetails();
			List<ServiceParameterDetailsDTO.Attribute> attributeDTOList = new ArrayList<>();

			if (specificAttributesDTO != null && specificAttributesDTO.getAttributes() != null) {

				for (SpecificAttributesDTO.Attribute attr : specificAttributesDTO.getAttributes()) {

					ServiceParameterDetailsDTO.Attribute attributeDTO = new ServiceParameterDetailsDTO.Attribute();

					attributeDTO.setAttributeId(createAttributeId(taskId, holderId, attr.getAttributeId()));
					attributeDTO.setAttributeLabel(attr.getAttributeLabel());
					attributeDTO.setTypeId(attr.getTypeId());
					attributeDTO.setType(ApplicationConstants.ATTRIBUTE);

					attributeDTOList.add(attributeDTO);
				}
			}

			templateDTO.setTemplateName(formDetail.getTemplateName());
			templateDTO.setTaskId(taskId);
			templateDTO.setAttributeList(attributeDTOList);

			templateList.add(templateDTO);

			responseDTO.setServiceId(serviceId);
			responseDTO.setTemplateDetails(templateList);
		}

		return ResponseEntity.ok(responseDTO);

	} catch (Exception ex) {
		if (ex instanceof SPRuntimeError) {
			throw ex;
		} else {
			ex.printStackTrace();
			metadataCreationLogger.error("Error : Get all process flow forms with attributes :", ex);
			throw new SPRuntimeError("Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}

    public ResponseEntity<?> getAllVersionOfService(Integer serviceId, UserSessionDTO userSessionDetails) {
		try {
			List<Integer> serviceVersions = new ArrayList<Integer>();
			List<ServiceDefinition> serviceDefinitions = serviceDefinitionRepository.findByBaseServiceId(serviceId/10000).orElseThrow(() -> new SPRuntimeError("Service not found", HttpStatus.BAD_REQUEST));
			for (ServiceDefinition serviceDefinition : serviceDefinitions) {
				Integer versionNo = serviceDefinition.getVersionNo();
				serviceVersions.add(versionNo);
			}
			return ResponseEntity.ok(Map.of("versions",serviceVersions));
		} catch (Exception ex) {
			ex.printStackTrace();
			if (ex instanceof SPRuntimeError) {
				throw ex;
			} else {
				metadataCreationLogger.error("Error : Get all version of service :", ex);
				throw new SPRuntimeError("Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
	}
    
	public JSONObject saveServiceDefinition(ServiceDefinitionDTO dto, UserSessionDTO userSessionDetails) {
		JSONObject responseJson = new JSONObject();
		try {
			responseJson.put("status", true);
			Integer serviceId = null;
			ServiceDefinition serviceDefinition = new ServiceDefinition();
			ServiceLog serviceLog = new ServiceLog();
			if (dto.getServiceId() != null && !dto.getServiceId().equals(0)) {

				serviceId = dto.getServiceId();
				serviceDefinition = serviceDefinitionRepository.findByServiceIdAndUserId(serviceId,
						userSessionDetails.getUserID().longValue());
				serviceLog = serviceLogRepository.findByServiceId(serviceId);

				if (Objects.isNull(serviceDefinition)) {
					responseJson.put("errorCode", "400");
					responseJson.put("errorMessage", "Invalid Service");
					responseJson.put("status", false);
					return responseJson;
				}

				
				if (dto.getName() != null && !dto.getName().isBlank() && serviceDefinitionRepository
						.existsByServiceNameIgnoreCaseAndServiceIdNot(dto.getName(), serviceId)) {
					responseJson.put("errorCode", "400");
					responseJson.put("errorMessage", "Service name already exists");
					responseJson.put("status", false);
					return responseJson;
				}

				if (dto.getAbbreviation() != null && !dto.getAbbreviation().isBlank()
						&& serviceDefinitionRepository.existsByServiceAbbreviationAndServiceIdNot(dto.getAbbreviation(), serviceId)) {
					responseJson.put("errorCode", "400");
					responseJson.put("errorMessage", "Service abbreviation already exists");
					responseJson.put("status", false);
					return responseJson;
				}

				if (serviceLog.getcDate() != null) {
					serviceLog.setuDate(new Date());
				} else {
					serviceLog.setcDate(new Date());
				}
				serviceLog.setTablist(serviceLog.getTablist());

			} else {

				if (dto.getName() != null && !dto.getName().isBlank()
						&& serviceDefinitionRepository.existsByServiceNameIgnoreCase(dto.getName())) {
					responseJson.put("errorCode", "400");
					responseJson.put("errorMessage", "Service name already exists");
					responseJson.put("status", false);
					return responseJson;
				}
				if (dto.getAbbreviation() != null && !dto.getAbbreviation().isBlank()
						&& serviceDefinitionRepository.existsByServiceAbbreviation(dto.getAbbreviation())) {
					responseJson.put("errorCode", "400");
					responseJson.put("errorMessage", "Service abbreviation already exists");
					responseJson.put("status", false);
					return responseJson;
				}

				SpeSequenceLastNo speSequenceLastNo = speSequenceLastNoRepo.getSequenceLastNo(1);

				serviceId = Integer.valueOf(String.valueOf(speSequenceLastNo.getFnc_generate_seq()).concat("0001"));

				serviceLog.setTablist("1");
			}

			serviceDefinition.setServiceId(serviceId);

			serviceDefinition.setServiceName(dto.getName());
			serviceDefinition.setServiceAbbreviation(dto.getAbbreviation());

			if (dto.getLogo() != null) {

				saveLogoFile(dto.getLogo(), userSessionDetails);
			}

			serviceDefinition.setDefinitionJson(dto);

			serviceDefinition.setcDate(new Date());

			serviceDefinition.setBaseServiceId(serviceId / 10000);

			serviceDefinition.setVersionNo(serviceId % 10000);

			serviceDefinition.setMinorVersionNo(0);

			serviceDefinition.setUserId(userSessionDetails.getUserID().longValue());

			serviceDefinition.setStateId(userSessionDetails.getClcId());

			serviceDefinition.setDepartmentId(userSessionDetails.getEntityId().longValue());

			serviceLog.setBaseServiceId(serviceId / 10000);

			serviceLog.setServiceId(serviceId);

			serviceLog.setServiceStatus(0);

			serviceLog.setLaunchFlag('N');

			serviceLog.setVersionNo(serviceId % 10000);

			serviceLog.setMinorVersionNo(0);

			serviceLog.setTenantId(userSessionDetails.getTenantId());

			responseJson = saveServiceEntity(responseJson, serviceDefinition, serviceLog);

			return responseJson;

		} catch (Exception ex) {

			ex.printStackTrace();

			responseJson.put("errorCode", "500");
			responseJson.put("errorMessage", "Internal Server Error");
			responseJson.put("status", false);

			return responseJson;
		}
	}
    @Transactional
    private JSONObject saveServiceEntity(JSONObject responseJson, ServiceDefinition serviceDefinition, ServiceLog serviceLog) {
        try {
            ServiceDefinition savedEntity = serviceDefinitionRepository.save(serviceDefinition);
            ServiceLog savedLogEntity = serviceLogRepository.save(serviceLog);
            responseJson.put("serviceId", savedEntity.getServiceId());
            responseJson.put("message","Service definition saved successfully");
            responseJson.put("tabList",savedLogEntity.getTablist());
            return responseJson;
        }catch(Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            responseJson.put("status", false);
            return responseJson;
        }
    }

    private void saveLogoFile(String uploadId, UserSessionDTO userSessionDetails) {
        try {
            List<String> uploadIds =new ArrayList<>();
            uploadIds.add(uploadId);
            ResponseEntity<CommitResponse> commitResponse = fileMgmtFeignClient.commitFiles(entityToString(userSessionDetails), new CommitRequest(uploadIds));
            CommitResponse body = commitResponse.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save logo file: " + e.getMessage(), e);
        }
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._\\-\\(\\)]", "_");
    }

    public ResponseEntity<?> getServiceDefinitionDetails(Integer serviceId, UserSessionDTO userSessionDetails) {
        JSONObject responseJson=new JSONObject();
        List<String> uploadIds=new ArrayList<>();
        try {
            ServiceDefinition serviceDefinition = serviceDefinitionRepository.findByServiceIdAndUserId(serviceId, userSessionDetails.getUserID().longValue());
            ServiceLog servicelog=serviceLogRepository.findByServiceId(serviceId);
            if (Objects.isNull(serviceDefinition)) {
                responseJson.put("errorCode", "400");
                responseJson.put("errorMessage", "Service not found");
                return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
            }

            ServiceDefinitionDTO dto = serviceDefinition.getDefinitionJson();
            //String checksum=CommonUtil.generateChecksums(serviceId.toString(), 2);
            dto.setServiceId(serviceDefinition.getServiceId());
            dto.setTablist(servicelog.getTablist());
            String uploadId=dto.getLogo();
            dto.setLogo("");
            if(uploadId!=null && !uploadId.isEmpty()) {
                uploadIds.add(uploadId);
                ResponseEntity<FileViewResponse> fileViewResp = fileMgmtFeignClient.filePreview(entityToString(userSessionDetails), new CommitRequest(uploadIds));
                FileViewResponse body = fileViewResp.getBody();
                if (isNull(body) || isNull(body.getResults())) {
                    throw new SPRuntimeError("Failed to fetch file",HttpStatus.FAILED_DEPENDENCY);
                }
                Map<String, Data> results = body.getResults();
                if (results.containsKey(uploadId)) {
                    dto.setLogo(mapper.writeValueAsString(results.get(uploadId)));
                }
            }
            return ResponseEntity.ok().body(dto);
        }catch(Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    public ResponseEntity<?> getServiceList(Integer status, UserSessionDTO userSessionDetails) {

        JSONObject responseJson = new JSONObject();
        List<ServiceListDetailsDTO> serviceListDetailsDTOs = new ArrayList<>();
        try {
            List<ServiceDefinition> serviceList =serviceDefinitionRepository.findByUserId(userSessionDetails.getUserID().longValue());
            if (serviceList.isEmpty()) {
                return ResponseEntity.ok(serviceListDetailsDTOs);
            }
            List<Integer> serviceIds = serviceList.stream().map(ServiceDefinition::getServiceId).collect(Collectors.toList());
            List<ServiceLog> serviceLogs =(status != null)? serviceLogRepository.findByServiceIdInAndServiceStatus(serviceIds, status)
                    :serviceLogRepository.findByServiceIdIn(serviceIds);

            Map<Integer, ServiceLog> serviceLogMap = serviceLogs.stream().collect(Collectors.toMap(ServiceLog::getServiceId,sl -> sl,(a, b) -> a));

            if (status != null) {
                serviceList = serviceList.stream().filter(sd -> serviceLogMap.containsKey(sd.getServiceId())).collect(Collectors.toList());
            }
            for (ServiceDefinition service : serviceList) {
                ServiceLog serviceLog = serviceLogMap.get(service.getServiceId());
                ServiceListDetailsDTO dto = new ServiceListDetailsDTO();
                dto.setServiceId(service.getServiceId());
                dto.setServiceName(service.getDefinitionJson().getName());
                dto.setStatus(serviceLog != null && serviceLog.getServiceStatus() != null
                        ? ServiceStatus.fromCode(serviceLog.getServiceStatus()).name()
                        : null);
                serviceListDetailsDTOs.add(dto);
            }

            return ResponseEntity.ok(serviceListDetailsDTOs);

        } catch (Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<?> getServiceTemplateList(Integer serviceId, UserSessionDTO userSessionDetails) {
        JSONObject responseJson=new JSONObject();
        List<ServiceTemplateDTO> serviceTemplateDTOList = new ArrayList<ServiceTemplateDTO>();
        try {
            List<ServiceTemplate> serviceTemplateList = serviceTemplateRepository.findByServiceIdAndUserId(serviceId,userSessionDetails.getUserID().longValue());
            for(ServiceTemplate  serviceTemplate:serviceTemplateList) {
                ServiceTemplateDTO serviceTemplateDTO = new ServiceTemplateDTO();
                serviceTemplateDTO.setServiceId(serviceTemplate.getServiceId());
                serviceTemplateDTO.setTemplateId(serviceTemplate.getId());
                serviceTemplateDTO.setTaskId(serviceTemplate.getTaskId());
                serviceTemplateDTO.setTemplateName(serviceTemplate.getTemplateName());
                serviceTemplateDTOList.add(serviceTemplateDTO);
            }
            return ResponseEntity.ok().body(serviceTemplateDTOList);
        }catch(Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }
    public ResponseEntity<?> getTaskList(Integer serviceId, String taskId, UserSessionDTO userSessionDetails) {
        JSONObject responseJson=new JSONObject();
        TaskDTO taskDto= new TaskDTO();
        List<TaskDTO.Task> taskList=new ArrayList<>();
        try {
            ServiceProcessFlow serviceProcessFlow = processFlowRepository.findByServiceIdAndUserId(serviceId, userSessionDetails.getUserID().longValue());
            if(serviceProcessFlow!=null &&serviceProcessFlow.getProcessFlowJson()!=null) {
                taskDto.setServiceId(serviceId);
                List<com.serviceplus.metadata.dto.ProcessFlowDTO.NodeDTO> nodes = serviceProcessFlow.getProcessFlowJson().getNodes();
                List<com.serviceplus.metadata.dto.ProcessFlowDTO.EdgeDTO> edges = serviceProcessFlow.getProcessFlowJson().getEdges();
                Map<String, NodeDTO> nodeMap = nodes.stream()
                        .collect(Collectors.toMap(NodeDTO::getId, Function.identity()));

                Map<String, List<String>> outgoingMap = edges.stream()
                        .collect(Collectors.groupingBy(
                                EdgeDTO::getSource,
                                Collectors.mapping(EdgeDTO::getTarget, Collectors.toList())
                        ));

                Map<String, List<String>> incomingMap = edges.stream()
                        .collect(Collectors.groupingBy(
                                EdgeDTO::getTarget,
                                Collectors.mapping(EdgeDTO::getSource, Collectors.toList())
                        ));
                List<NodeDTO> filteredNodes = nodes.stream()
                        .filter(node -> taskId == null || taskId.toString().equals(node.getId()))
                        .collect(Collectors.toList());
                for(com.serviceplus.metadata.dto.ProcessFlowDTO.NodeDTO node:filteredNodes) {
                    TaskDTO.Task task=new Task();
                    task.setTaskId(node.getId());
                    task.setTaskName(node.getData()!=null ? node.getData().getName():"");
                    task.setTaskType(node.getType());
                    Set<String> nextTaskIds = new LinkedHashSet<>();
                    collectNextTasks(node.getId(), outgoingMap, nodeMap, new HashSet<>(), nextTaskIds);

                    List<TaskDTO.Task.TaskReference> nextTasks = nextTaskIds.stream()
                            .map(id -> {
                            	TaskDTO.Task.TaskReference ref = new TaskDTO.Task.TaskReference();
                                ref.setTaskId(id);

                                NodeDTO nextNode = nodeMap.get(id);
                                ref.setTaskName(nextNode != null && nextNode.getData() != null
                                        ? nextNode.getData().getName()
                                        : "");

                                return ref;
                            })
                            .collect(Collectors.toList());

                    task.setNextTasks(nextTasks);
                    Set<String> previousTaskIds = new LinkedHashSet<>();
                    collectPreviousTasks(node.getId(), incomingMap, nodeMap, new HashSet<>(), previousTaskIds);

                    List<TaskDTO.Task.TaskReference> prevTasks = previousTaskIds.stream()
                            .map(id -> {
                            	TaskDTO.Task.TaskReference ref = new TaskDTO.Task.TaskReference();
                                ref.setTaskId(id);

                                NodeDTO prevNode = nodeMap.get(id);
                                ref.setTaskName(prevNode != null && prevNode.getData() != null
                                        ? prevNode.getData().getName()
                                        : "");

                                return ref;
                            })
                            .collect(Collectors.toList());

                    task.setPrevTasks(prevTasks);
                    taskList.add(task);
                }
                taskDto.setTaskList(taskList);
            }
            return ResponseEntity.ok().body(taskDto);
        }catch(Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }
    public ResponseEntity<?> saveServiceTemplate(ServiceTemplateDTO serviceTemplateDTO, UserSessionDTO userSessionDetails) {
        JSONObject responseJson = new JSONObject();
        try {
            String formId=serviceTemplateDTO.getFormId();
            String holderId=serviceTemplateDTO.getHolderId();
            Integer serviceId=serviceTemplateDTO.getServiceId();
            ServiceTemplate serviceTemplate=null;
            if(serviceTemplateDTO.getTemplateId()!=null) {
                serviceTemplate=serviceTemplateRepository.findByIdAndUserId(serviceTemplateDTO.getTemplateId(),userSessionDetails.getUserID().longValue());
                if(Objects.isNull(serviceTemplate)) {
                    responseJson.put("errorCode", "400");
                    responseJson.put("errorMessage", "Invalid Service");
                    return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
                }
                serviceTemplate.setuDate(new Date());
                if(serviceTemplate.getInstantiableFlag().equalsIgnoreCase("N") && serviceTemplateDTO.getInstantiableFlag().equalsIgnoreCase("N")) {
                    serviceTemplateDTO.setInstantiableFlag(null);
                }else {
                    serviceTemplate.setInstantiableFlag(serviceTemplateDTO.getInstantiableFlag());
                }
            }else {
                serviceTemplate=new ServiceTemplate();
                serviceTemplate.setcDate(new Date());
                serviceTemplate.setInstantiableFlag(serviceTemplateDTO.getInstantiableFlag());
            }
            ServiceLog serviceLog=serviceLogRepository.findByServiceId(serviceId);
            if(Objects.isNull(serviceLog)) {
                responseJson.put("errorCode", "400");
                responseJson.put("errorMessage", "Invalid Service");
                return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
            }
            serviceTemplate.setServiceId(serviceId);
            //serviceTemplate.setTemplateJson(serviceTemplateDTO);
            serviceTemplate.setVersionNo(serviceId%10000);
            serviceTemplate.setMinorVersion(0);
            serviceTemplate.setUserId(userSessionDetails.getUserID().longValue());
            serviceTemplate.setBaseServiceId(serviceId/10000);
            if(serviceTemplateDTO.getInstantiableFlag()!=null) {

                ServiceTemplateMappingDTO templateMappingDto=new ServiceTemplateMappingDTO();
                templateMappingDto.setCreatedBy(userSessionDetails.getUserID().longValue());
                templateMappingDto.setDepartmentId(userSessionDetails.getEntityId());
                templateMappingDto.setStateId(userSessionDetails.getClcId());
                templateMappingDto.setLocationId(userSessionDetails.getLocationId());
                templateMappingDto.setInstantiable(serviceTemplateDTO.getInstantiableFlag());
                templateMappingDto.setFormId(formId);


                ResponseEntity<Map<String, Object>> saveInstantiableForm = formFeignClient.saveInstantiableForm(templateMappingDto);
                if(saveInstantiableForm.getStatusCode().equals(HttpStatus.OK)) {
                    Map<String, Object> responseBody = saveInstantiableForm.getBody();
                    formId=(String) responseBody.get("formId");
                }
            }
            serviceTemplate.setFormId(formId);
            serviceTemplate.setHolderId(holderId);
            serviceTemplate.setTaskId(serviceTemplateDTO.getTaskId());
            responseJson=saveTemplate(responseJson, serviceTemplate, serviceLog);
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.OK);


        } catch (Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @Transactional
    private JSONObject saveTemplate(JSONObject responseJson, ServiceTemplate serviceTemplate, ServiceLog serviceLog) {
        try {
            String tablist=serviceLog.getTablist();
            ServiceTemplate savedEntity = serviceTemplateRepository.save(serviceTemplate);
            if(!tablist.contains("6")) {
                serviceLog.setTablist(serviceLog.getTablist()+",6");
                ServiceLog savedLogEntity = serviceLogRepository.save(serviceLog);
                tablist=savedLogEntity.getTablist();
            }
            responseJson.put("templateId", savedEntity.getId());
            responseJson.put("message", "Service Tempalte saved successfully");
            responseJson.put("tabList", tablist);
            return responseJson;
        } catch (Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return responseJson;
        }
    }
    public ResponseEntity<?> getServiceTemplate(Integer serviceId, Integer templateId,
                                                UserSessionDTO userSessionDetails) {
        JSONObject responseJson=new JSONObject();
        ServiceTemplateDTO serviceTemplateDTO = new ServiceTemplateDTO();
        try {
            ServiceTemplate serviceTemplate=serviceTemplateRepository.findByIdAndServiceIdAndUserId(templateId,serviceId,userSessionDetails.getUserID().longValue());
            if(Objects.nonNull(serviceTemplate)) {
                serviceTemplateDTO.setTemplateId(serviceTemplate.getId());
                serviceTemplateDTO.setFormId(serviceTemplate.getFormId());
                serviceTemplateDTO.setTemplateName(serviceTemplate.getTemplateName());
            }
            return ResponseEntity.ok().body(serviceTemplateDTO);
        }catch(Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }
    public void deleteTemplate(Integer serviceId, String nodeId, UserSessionDTO userSessionDetails) {
        try {
            ServiceTemplate serviceTemplate=serviceTemplateRepository.findByServiceIdAndTaskIdAndUserId(serviceId,nodeId,userSessionDetails.getUserID().longValue());
            if(Objects.nonNull(serviceTemplate)) {
                serviceTemplateRepository.delete(serviceTemplate);
            }
        }catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> getServiceFormAttributeList(Integer serviceId, UserSessionDTO userSessionDetails) {
        JSONObject responseJson = new JSONObject();
        List<ServiceFormAttributeDetailsDTO> serviceFormAttrDetailDTOList = new ArrayList<ServiceFormAttributeDetailsDTO>();
        try {

            List<ServiceTemplate> serviceTemplateList = serviceTemplateRepository.findByServiceIdAndUserId(serviceId,userSessionDetails.getUserID().longValue());
            for (ServiceTemplate serviceTemplate : serviceTemplateList) {
                ServiceFormAttributeDetailsDTO serviceFormAttrDetailDTO = new ServiceFormAttributeDetailsDTO();
                serviceFormAttrDetailDTO.setTemplateName(serviceTemplate.getTemplateName());
                serviceFormAttrDetailDTO.setTemplateId(serviceTemplate.getId());
                ResponseEntity<?> formDtoResponse = formFeignClient.getAllAttributesByFormId(serviceTemplate.getFormId());
                if (formDtoResponse.getStatusCode().equals(HttpStatus.OK)) {
                    Map<Object,Object> responseObj = (Map<Object, Object>) formDtoResponse.getBody();
                    JSONObject json=new JSONObject(responseObj);
                    FormDTO formDto= new Gson().fromJson(json.toString(), FormDTO.class);
                    serviceFormAttrDetailDTO.setTemplateName(serviceTemplate.getTemplateName());
                    formDto.setFormId(serviceTemplate.getFormId());
                    List<Attribute> attrList = formDto.getAttributes();
                    for(Attribute attr:attrList) {
                        String attrId=createAttributeId(serviceTemplate.getTaskId(),serviceTemplate.getHolderId(),attr.getAttributeId());
                        attr.setAttributeId(attrId);
                    }
                    serviceFormAttrDetailDTO.setAttributeList(attrList);
                }
                serviceFormAttrDetailDTOList.add(serviceFormAttrDetailDTO);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
        return ResponseEntity.ok().body(serviceFormAttrDetailDTOList);
    }
    public ResponseEntity<?> saveServiceOutputFormat(@Valid ServiceOutputFormatDTO serviceOutputFormatDTO,
                                                     UserSessionDTO userSessionDetails) {
        JSONObject responseJson = new JSONObject();
        try {
            Integer serviceId=serviceOutputFormatDTO.getServiceId();
            Long outputFormatId=serviceOutputFormatDTO.getOutputFormatId();
            ServiceOutputFormat serviceOutputFormat=null;
            if(outputFormatId!=null) {
                serviceOutputFormat=serviceOutputFormatRepository.findByIdAndUserId(outputFormatId,userSessionDetails.getUserID().longValue());
                if(Objects.isNull(serviceOutputFormat)) {
                    responseJson.put("errorCode", "400");
                    responseJson.put("errorMessage", "Invalid Service");
                    return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
                }
                serviceOutputFormat.setUDate(new Date());
            }else {
                serviceOutputFormat=new ServiceOutputFormat();
                serviceOutputFormat.setCDate(new Date());
            }
            ServiceLog serviceLog=serviceLogRepository.findByServiceId(serviceId);
            if(Objects.isNull(serviceLog)) {
                responseJson.put("errorCode", "400");
                responseJson.put("errorMessage", "Invalid Service");
                return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
            }
            serviceOutputFormat.setServiceId(serviceId);
            serviceOutputFormat.setOutputFormatJson(serviceOutputFormatDTO);
            serviceOutputFormat.setVersionNo(serviceId%10000);
            serviceOutputFormat.setMinorVersion(0);
            serviceOutputFormat.setUserId(userSessionDetails.getUserID().longValue());
            serviceOutputFormat.setBaseServiceId(serviceId/10000);


            responseJson=saveOutputFormat(responseJson, serviceOutputFormat, serviceLog);
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.OK);


        } catch (Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private JSONObject saveOutputFormat(JSONObject responseJson, ServiceOutputFormat serviceOutputFormat,
                                        ServiceLog serviceLog) {
        try {
            String tablist = serviceLog.getTablist();
            ServiceOutputFormat savedEntity = serviceOutputFormatRepository.save(serviceOutputFormat);
            if (!tablist.contains("7")) {
                serviceLog.setTablist(serviceLog.getTablist() + ",7");
                ServiceLog savedLogEntity = serviceLogRepository.save(serviceLog);
                tablist = savedLogEntity.getTablist();
            }
            responseJson.put("outputFormatId", savedEntity.getId());
            responseJson.put("message", "Output format saved successfully");
            responseJson.put("tabList", tablist);
            return responseJson;
        } catch (Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return responseJson;
        }
    }
    public ResponseEntity<?> getServiceOutputFormat(Integer serviceId, Long outputFormatId,
                                                    UserSessionDTO userSessionDetails) {
        JSONObject responseJson=new JSONObject();
        ServiceOutputFormatDTO serviceOutputFormatDTO = null;
        try {
            ServiceOutputFormat serviceOutputFormat=serviceOutputFormatRepository.findByIdAndServiceIdAndUserId(outputFormatId,serviceId,userSessionDetails.getUserID().longValue());
            if(Objects.nonNull(serviceOutputFormat)) {
                serviceOutputFormatDTO=serviceOutputFormat.getOutputFormatJson();
            }
            return ResponseEntity.ok().body(serviceOutputFormatDTO);
        }catch(Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    public ResponseEntity<?> deleteOutputFormat(Integer serviceId, Long outputFormatId, UserSessionDTO userSessionDetails) {
        JSONObject responseJson=new JSONObject();
        try {
            ServiceOutputFormat serviceOutputFormat=serviceOutputFormatRepository.findByIdAndServiceIdAndUserId(outputFormatId,serviceId,userSessionDetails.getUserID().longValue());
            if(Objects.nonNull(serviceOutputFormat)) {
                serviceOutputFormatRepository.delete(serviceOutputFormat);
                responseJson.put("message", "Output Format deleted successfully.");
            }else {
                responseJson.put("errorCode", "400");
                responseJson.put("errorMessage", "Output Format does not exist.");
                new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
            }
            return ResponseEntity.ok().body(responseJson.toString());
        }catch(Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }
    public ResponseEntity<?> getServiceOutputFormatList(Integer serviceId, UserSessionDTO userSessionDetails) {
        JSONObject responseJson=new JSONObject();
        List<ServiceOutputFormatDTO> serviceOutputFormatDTOList = new ArrayList<ServiceOutputFormatDTO>();
        try {
            List<ServiceOutputFormat> serviceOutputFormatList = serviceOutputFormatRepository.findByServiceIdAndUserId(serviceId,userSessionDetails.getUserID().longValue());
            for(ServiceOutputFormat  serviceOutputFormat:serviceOutputFormatList) {
                ServiceOutputFormatDTO serviceOutputFormatDTO = new ServiceOutputFormatDTO();
                serviceOutputFormatDTO.setServiceId(serviceOutputFormat.getServiceId());
                serviceOutputFormatDTO.setOutputFormatId(serviceOutputFormat.getId());
                serviceOutputFormatDTO.setTemplateName(serviceOutputFormat.getOutputFormatJson().getTemplateName());
                serviceOutputFormatDTOList.add(serviceOutputFormatDTO);
            }
            return ResponseEntity.ok().body(serviceOutputFormatDTOList);
        }catch(Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    public ResponseEntity<?> getXSDAttributeList(String xsd) {
        JSONObject responseJson = new JSONObject();
        List<XSDDetail> xsdDetailList = new ArrayList<XSDDetail>();
        try {
            xsd = xsd.trim();
            String decodedXsd = URLDecoder.decode(xsd, StandardCharsets.UTF_8.toString());
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                try {
                    Document doc = docBuilder.parse(new InputSource(new StringReader(decodedXsd)));
                    NodeList elementList = doc.getElementsByTagName("xs:element");
                    NodeList attrList = doc.getElementsByTagName("xs:attribute");
                    for (int i = 0; i < elementList.getLength(); i++) {
                        boolean hasChild = false;
                        Element first = (Element) elementList.item(i);
                        if (first.hasAttributes()) {
                            XSDDetail xsdDetail = new XSDDetail();

                            String parentNodeNm = null;

                            Node pNode = first.getParentNode();
                            String pNodeNm = pNode.getNodeName();
                            if (pNodeNm.contains("sequence")) {
                                pNode = pNode.getParentNode();
                                pNodeNm = pNode.getNodeName();
                                if (pNodeNm.contains("complexType")) {
                                    pNode = pNode.getParentNode();
                                    pNodeNm = pNode.getNodeName();
                                    if (pNodeNm.contains("element")) {
                                        parentNodeNm = ((Element) pNode).getAttribute("name");
                                    }
                                }
                            }
                            parentNodeNm = parentNodeNm == null || parentNodeNm.equalsIgnoreCase("null") ? ""
                                    : parentNodeNm;
                            String elementName = first.getAttribute("name");
                            hasChild = first.hasChildNodes();
                            String nodeType = "E";

                            xsdDetail.setParent(parentNodeNm);
                            xsdDetail.setHasChild(hasChild);
                            xsdDetail.setNode(elementName);
                            if (hasChild) {
                                nodeType = "D";
                                xsdDetail.setAttrId("0");
                            }
                            xsdDetail.setType(nodeType);

                            xsdDetailList.add(xsdDetail);
                        }
                    }
                    for (int i = 0; i < attrList.getLength(); i++) {
                        Element first = (Element) attrList.item(i);
                        if (first.hasAttributes()) {
                            String attrName = first.getAttribute("name");
                            XSDDetail xsdDetail = new XSDDetail();
                            xsdDetail.setNode(attrName);
                            xsdDetailList.add(xsdDetail);
                        }
                    }

                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            return ResponseEntity.ok().body(xsdDetailList);
        } catch (Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }
    public ResponseEntity<?> getServiceAbbreviation(Integer serviceId, UserSessionDTO userSessionDetails) {
        JSONObject responseJson = new JSONObject();
        try {
            ServiceMetadata serviceMetadata = metadataRepository.findById(serviceId).get();
            if (Objects.nonNull(serviceMetadata)) {
                responseJson.put("abbr", serviceMetadata.getMetadataJson().getServiceAbbrevation());
            } else {
                responseJson.put("errorCode", "400");
                responseJson.put("errorMessage", "Invalid service");
                new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
            }
            return ResponseEntity.ok().body(responseJson.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }
    public ResponseEntity<?> getEligibleServiceList(UserSessionDTO userSessionDetails) {
        List<EligibleServiceDTO> eligibleServiceDTO=new ArrayList<EligibleServiceDTO>();
        try {
            List<Roles> userRoles = userSessionDetails.getRoles();
            Set<String> allowedSubmissionModes = userRoles.stream()
                    .map(role -> ApplicationConstants.ROLE_TO_SUBMISSION_MODE.get(role.getRoleId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (allowedSubmissionModes.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            List<ServiceLog> serviceLogs = serviceLogRepository.findByServiceStatus(ServiceStatus.FROZEN.getCode());
            if (serviceLogs.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            List<Integer> serviceIds = serviceLogs.stream()
                    .collect(Collectors.toMap(
                            ServiceLog::getBaseServiceId,
                            ServiceLog::getServiceId,
                            Integer::max))
                    .values().stream().toList();

            List<ServiceMetadata> serviceList = metadataRepository.findByTenantIdAndServiceIdIn(
                    userSessionDetails.getTenantId(),
                    serviceIds);

            for (ServiceMetadata serviceMeta : serviceList) {
                ServiceJSONDTO serviceJson = serviceMeta.getMetadataJson();
                if (serviceJson == null || serviceJson.getSubmissionModes() == null) {
                    continue;
                }

                boolean matches = serviceJson.getSubmissionModes().stream()
                        .anyMatch(mode -> mode.getValue() != null && allowedSubmissionModes.contains(mode.getValue().toString()));

                if (!matches) {
                    continue;
                }

                EligibleServiceDTO dto = new EligibleServiceDTO();
                dto.setBaseServiceId(serviceJson.getServiceId() / 10000);
                dto.setServiceId(serviceJson.getServiceId());
                dto.setServiceName(serviceJson.getServiceName());
                dto.setDepartmentName(serviceJson.getDepartmentName());
                dto.setStateName(serviceJson.getStateName());
                dto.setFormId(serviceJson.getApplFormId());
                dto.setTaskId(serviceJson.getApplSubmissionTaskId());
                dto.setTaskType("A");

                List<ActivityMapDTO> activities = serviceJson.getActivityMap().stream()
                        .filter(activity -> activity.getTaskId().equals(serviceJson.getApplSubmissionTaskId()))
                        .collect(Collectors.toList());

                List<OfficeDetailsDTO> locations = serviceJson.getOfficeDetails().stream()
                        .filter(location -> location.getTaskId().equals(serviceJson.getApplSubmissionTaskId()))
                        .collect(Collectors.toList());

                dto.setActivityMap(activities.isEmpty() ? new ActivityMapDTO() : activities.get(0));
                dto.setLocations(locations.isEmpty()
                        ? new ArrayList<>()
                        : locations.get(0).getAllowedOffices());

                eligibleServiceDTO.add(dto);
            }
            return ResponseEntity.ok(eligibleServiceDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching eligible services");
        }
    }
    
    public ResponseEntity<?> saveServiceCharge(ServiceChargeDTO serviceChargeDTO, UserSessionDTO userSessionDetails) {
        JSONObject responseJson = new JSONObject();
        try {
            Integer serviceId=serviceChargeDTO.getServiceId();
            Long chargeDetailId=serviceChargeDTO.getChargeDetailId();
            ServiceChargeDetail serviceCharge=null;
            if(chargeDetailId!=null) {
                serviceCharge=serviceChargeRepository.findByIdAndUserId(chargeDetailId,userSessionDetails.getUserID().longValue());
                if(Objects.isNull(serviceCharge)) {
                    responseJson.put("errorCode", "400");
                    responseJson.put("errorMessage", "Invalid Service");
                    return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
                }
                serviceCharge.setUDate(new Date());
            }else {
                serviceCharge=new ServiceChargeDetail();
                serviceCharge.setCDate(new Date());
            }
            ServiceLog serviceLog=serviceLogRepository.findByServiceId(serviceId);
            if(Objects.isNull(serviceLog)) {
                responseJson.put("errorCode", "400");
                responseJson.put("errorMessage", "Invalid Service");
                return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
            }
            
            List<PaymentModeDTO> paymentModes = serviceChargeDTO.getPaymentModes();
            
            ServiceMasterData masterData = masterDataRepository.findByName("Payment Mode").orElseThrow(() -> new RuntimeException("Payment Mode  not found"));
            List<PaymentModeDTO> validatePaymentModes = new ArrayList<ServiceChargeDTO.PaymentModeDTO>();
            
            for(PaymentModeDTO paymentMode : paymentModes) {
            	if(paymentMode!=null) {
            		List<Map<String, Object>> dataList = mapper.readValue(masterData.getData(), new TypeReference<List<Map<String, Object>>>(){});
            		
            		List<Map<String, Object>> filteredDataList = dataList.stream()
            				.filter(item -> {
            					Object value = item.get("value");
            					return value != null && Integer.parseInt(value.toString())==paymentMode.getValue();
            				}).collect(Collectors.toList());
            		if(filteredDataList.isEmpty()) {
            			responseJson.put("errorCode", "400");
            			responseJson.put("errorMessage", "Invalid Payment Mode Data :"+paymentMode.getLabel());
            			return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
            		}else {
            			paymentMode.setOnlineFlag((Boolean) filteredDataList.get(0).get("online"));
            			paymentMode.setLabel((String) filteredDataList.get(0).get("label"));
            			paymentMode.setValue((Integer) filteredDataList.get(0).get("value"));
            		}
            		validatePaymentModes.add(paymentMode);
            	}
            }

            boolean isDuplicateTemplateName = serviceChargeRepository.existsByServiceIdAndTemplateName(serviceId,serviceChargeDTO.getTemplateName(),serviceChargeDTO.getChargeDetailId());
            if(isDuplicateTemplateName) {
            	responseJson.put("errorCode", "400");
    			responseJson.put("errorMessage", "Template Name is already present");
    			return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
            }
            
            serviceChargeDTO.setPaymentModes(validatePaymentModes);
            serviceCharge.setServiceChargeJson(serviceChargeDTO);
            serviceCharge.setServiceId(serviceId);
            serviceCharge.setServiceChargeJson(serviceChargeDTO);
            serviceCharge.setVersionNo(serviceId%10000);
            serviceCharge.setMinorVersion(0);
            serviceCharge.setUserId(userSessionDetails.getUserID().longValue());
            serviceCharge.setBaseServiceId(serviceId/10000);


            responseJson=saveChargeDetail(responseJson, serviceCharge, serviceLog);
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.OK);


        } catch (Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    private JSONObject saveChargeDetail(JSONObject responseJson, ServiceChargeDetail serviceCharge,
                                        ServiceLog serviceLog) {
        try {
            String tablist = serviceLog.getTablist();
            ServiceChargeDetail savedEntity = serviceChargeRepository.save(serviceCharge);
            if (!tablist.contains("8")) {
                serviceLog.setTablist(serviceLog.getTablist() + ",8");
                ServiceLog savedLogEntity = serviceLogRepository.save(serviceLog);
                tablist = savedLogEntity.getTablist();
            }
            responseJson.put("outputFormatId", savedEntity.getId());
            responseJson.put("message", "Output format saved successfully");
            responseJson.put("tabList", tablist);
            return responseJson;
        } catch (Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return responseJson;
        }
    }
    public ResponseEntity<?> getServiceChargeDetail(Integer serviceId, Long chargeDetailId, UserSessionDTO userSessionDetails) {
        JSONObject responseJson=new JSONObject();
        ServiceChargeDTO serviceChargeDTO = null;
        try {
            ServiceChargeDetail serviceChargeDetail=serviceChargeRepository.findByIdAndServiceIdAndUserId(chargeDetailId,serviceId,userSessionDetails.getUserID().longValue());
            if(Objects.nonNull(serviceChargeDetail)) {
                serviceChargeDTO=serviceChargeDetail.getServiceChargeJson();
            }
            serviceChargeDTO.setChargeDetailId(serviceChargeDetail.getId());
            return ResponseEntity.ok().body(serviceChargeDTO);
        }catch(Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }
    public ResponseEntity<?> getServiceChargeDetailList(Integer serviceId, UserSessionDTO userSessionDetails) {
        JSONObject responseJson=new JSONObject();
        List<ServiceChargeDTO> serviceChargeDTOList = new ArrayList<ServiceChargeDTO>();
        try {
            List<ServiceChargeDetail> serviceChargeDetailList = serviceChargeRepository.findByServiceIdAndUserId(serviceId, userSessionDetails.getUserID().longValue());
            for(ServiceChargeDetail serviceChargeDetail:serviceChargeDetailList) {
            	serviceChargeDetail.getServiceChargeJson().setChargeDetailId(serviceChargeDetail.getId());
                serviceChargeDTOList.add(serviceChargeDetail.getServiceChargeJson());
            }
            return ResponseEntity.ok().body(serviceChargeDTOList);
        }catch(Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }
    public ResponseEntity<?> deleteChargeDetail(Integer serviceId, Long chargeDetailId,UserSessionDTO userSessionDetails) {
        JSONObject responseJson=new JSONObject();
        try {
            ServiceChargeDetail serviceChargeDetail=serviceChargeRepository.findByIdAndServiceIdAndUserId(chargeDetailId,serviceId,userSessionDetails.getUserID().longValue());
            if(Objects.nonNull(serviceChargeDetail)) {
            	List<String> taskNames = processFlowRepository.findTasksUsingChargeDetail(serviceId, serviceChargeDetail.getId());
            	if (!taskNames.isEmpty()) {
            	    String tasks = String.join(", ", taskNames);
            	    responseJson.put("errorCode", "400");
            	    responseJson.put("errorMessage", "This charge detail cannot be deleted because it is being used in the following task(s): "
            	        + tasks + ". Please remove the charge detail from the payment configuration before deleting it.");
            	    return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
            	}
                serviceChargeRepository.delete(serviceChargeDetail);
                responseJson.put("message", "Charge Detail deleted successfully.");
            }else {
                responseJson.put("errorCode", "400");
                responseJson.put("errorMessage", "Charge Detail does not exist.");
                return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
            }
            return ResponseEntity.ok().body(responseJson.toString());
        }catch(Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }
    public ResponseEntity<?> freezeService(Integer serviceId, UserSessionDTO userSessionDetails) {
        try {
            ServiceLog serviceLog = validateAndGetServiceLog(serviceId);
            Set<String> mvelUsedAttrSet=new HashSet<String>();
            Set<String> documentUsedAttrSet=new HashSet<String>();
            ServiceDefinition serviceDefinition = serviceDefinitionRepository.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service Definition not found"));
            ServiceDeliveryUnitDefinition serviceDeliveryUnitDefinition=serviceDeliveryUnitRepo.findByServiceId(serviceId);
            List<ServiceDeliveryUnitMappingDTO> serviceDeliveryUnitMappingDetails=new ArrayList<>();
            ServiceProcessFlow processFlow =validateAndGetProcessFlow(serviceId, userSessionDetails.getUserID().longValue());
            List<ServiceOutputFormat> serviceOutputFormat=serviceOutputFormatRepository.findByServiceId(serviceId);
            List<MvelFunction> mvelFunctionList=mvelFunctionRepository.findByServiceId(serviceId);
            List<NodeDTO> nodes = processFlow.getProcessFlowJson().getNodes();
            List<EdgeDTO> edges = processFlow.getProcessFlowJson().getEdges();
            Map<Object,List<OfficeDetailsDTO.OfficeUnitData>> entityLevelOfficeUnitMap=new HashMap<Object,List<OfficeDetailsDTO.OfficeUnitData>>();
            NodeDTO submissionNode = getSubmissionNode(nodes);

            List<AuaApiConfigurationDTO> auaApiConfigurations = buildAuaApiConfigurations(serviceId, userSessionDetails.getUserID().longValue(), userSessionDetails.getTenantId());

            List<WorkFlowDataDTO> workFlowDetails = buildWorkFlowData(nodes);
            List<OfficeDetailsDTO> officeDetails = buildOfficeData(nodes,entityLevelOfficeUnitMap);
            Map<String, String> taskFormMapping = buildTaskFormMapping(nodes);
            ServiceProcessFlowDTO processFlowDTO = buildProcessFlowMap(serviceId,nodes,edges,officeDetails,taskFormMapping);
            List<ServiceProcessFlowDTO.AssociatedActivity> associatedActivities =buildAssociatedActivities(nodes,processFlow.getProcessFlowJson().getAssociatedTasks(),officeDetails,entityLevelOfficeUnitMap);
            processFlowDTO.setAssociatedActivities(associatedActivities);

            List<DocumentGenerationDetails> documentGenerationDetails = buildDocumentGenerationDetails(nodes);

            List<NotificationConfiguration> notificationConfigurations = notificationRepository.findByTenantIdAndServiceId(userSessionDetails.getTenantId(), serviceId);
            List<NotificationDetails> notificationDetails = buildNotificationDetails(notificationConfigurations);

            Set<String> notificationUsedAttrSet=getAttributeUsedInNotification(notificationConfigurations);

            Set<String> inboxSentboxFiltersUsedAttrList = new HashSet<String>();
            List<InboxSentBoxFilter> inboxSentBoxFilters = inboxSentBoxFilterRepository.findByServiceIdAndStatus(serviceId.longValue(), Status.ACTIVE.getValue());
            if(!inboxSentBoxFilters.isEmpty()) {
            		buildInboxSentboxFilters(inboxSentBoxFilters, inboxSentboxFiltersUsedAttrList);
            		FilterKafkaMessage filterKafkaMessage = buildInboxSentBoxFilterConfigKafkaMessage(inboxSentBoxFilters);
            		metadataCreationLogger.info("Info : Send data to Kafka InboxSentBoxFilterConfigKafkaMessage, serviceId : {}", serviceId);
            		kafkaProducer.sendMessage(filterConfigurationTopic, String.valueOf(serviceId), filterKafkaMessage);
            		metadataCreationLogger.info("Info : Send Successfully data to Kafka InboxSentBoxFilterConfigKafkaMessage, serviceId : {}", serviceId);
            }
            
            List<ActivityMapDTO> activityFlowMap = buildActivityFlow(nodes,notificationConfigurations);
            documentUsedAttrSet= getAttributeListUsedInService(serviceOutputFormat);
            mvelUsedAttrSet=getAttributeUsedInMvel(mvelFunctionList);
            List<MvelDetailsDTO> mvelDetailsDTO = buildMvelDetails(mvelFunctionList);
            List<ExternalAttributeMappingDTO> externalAttributeMappingDTO =buildExternalSystem(nodes);
            List<OutputFormatFreezeDTO> outputFormatDetail=getOutputFormatDetails(serviceOutputFormat);
            List<WebServiceTaskDTO> webServiceTasks=buildWebServiceTasks(nodes);
            List<EscalationDetailsDTO> escalationDetails= buildEscalationDetails(nodes);
            List<TimerTaskDTO> timerTaskDetails=buildTimerTaskDTO(nodes);
            
			if (serviceDeliveryUnitDefinition != null) {
				List<ServiceDeliveryUnitJSON> serviceDeliveryUnits = serviceDeliveryUnitDefinition
						.getServiceDeliveryUnits();
				serviceDeliveryUnitMappingDetails = buildServiceDeliveryUnitMappingWithUser(serviceDeliveryUnits,
						entityLevelOfficeUnitMap);
			}
			updateOfficeLevelIdsWithServiceDeliveryUnit(officeDetails, serviceDeliveryUnitMappingDetails);
        
            ServiceMetadata metadata = metadataRepository.findById(serviceId)
                    .orElse(new ServiceMetadata(serviceLog));

            updateMetadataJson(metadata,serviceDefinition,submissionNode,processFlowDTO,taskFormMapping,activityFlowMap,
                    officeDetails,documentUsedAttrSet,workFlowDetails,mvelDetailsDTO,mvelUsedAttrSet,
                    externalAttributeMappingDTO,outputFormatDetail,webServiceTasks,escalationDetails,timerTaskDetails,documentGenerationDetails,serviceDeliveryUnitMappingDetails, notificationDetails, notificationUsedAttrSet
                    , inboxSentboxFiltersUsedAttrList,auaApiConfigurations);

            serviceLog.setServiceStatus(1);
            serviceLogRepository.save(serviceLog);
            metadataRepository.save(metadata);
            List<NotificationConfigurationKafkaDTO> kafkaConfigurations = buildNotificationConfigurationKafkaDTOs(
					notificationConfigurations);

			if (!kafkaConfigurations.isEmpty()) {
				kafkaProducer.sendMessage(notificationConfigurationTopic, String.valueOf(serviceId),
						kafkaConfigurations);
			}
            RecommendedServiceFormKafkaDTO recommendedFormKafkaDTO = new RecommendedServiceFormKafkaDTO();
            recommendedFormKafkaDTO.setServiceId(serviceId);
            recommendedFormKafkaDTO.setServiceName(serviceDefinition.getDefinitionJson().getName());
            recommendedFormKafkaDTO.setServiceDesc(serviceDefinition.getDefinitionJson().getDescription());
            recommendedFormKafkaDTO.setForms(getFormIdAndName(nodes));
            recommendedFormKafkaDTO.setDepartmentId(metadata.getMetadataJson().getDepartmentId());
            recommendedFormKafkaDTO.setTenantId(metadata.getTenantId());
            formKafkaProducer.publishRecommendedForm(recommendedFormKafkaDTO);

            return ResponseEntity.ok(Map.of("message", "Service frozen successfully"));

        } catch (RuntimeException ex) {
            ex.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error","Internal Server Error"));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal Server Error"));
        }
    }

    private List<AuaApiConfigurationDTO> buildAuaApiConfigurations(Integer serviceId, Long userId, String tenantId) {

        metadataCreationLogger.info(
                "Starting AUA metadata creation. serviceId={}, userId={}, tenantId={}",
                serviceId, userId, tenantId
        );

        List<AuaApiConfigurationDTO> result = new ArrayList<>();

        List<AuaApiFieldMapping> mappings = mappingRepository.findByServiceIdAndCreatedByAndTenantId(serviceId, userId, tenantId);

        metadataCreationLogger.info(
                "AUA field mappings fetched. count={}",
                mappings == null ? 0 : mappings.size()
        );

        if (mappings == null || mappings.isEmpty()) {
            metadataCreationLogger.warn(
                    "No AUA field mappings found. serviceId={}, userId={}, tenantId={}",
                    serviceId, userId, tenantId
            );
            return result;
        }


        Map<Long, List<AuaApiFieldMapping>> mappingsByApi = mappings.stream()
                                                        .collect(
                                                                Collectors.groupingBy(mapping ->
                                                                                mapping.getApiDefinition().getId(),LinkedHashMap::new, Collectors.toList())
                                                        );

        for (Map.Entry<Long, List<AuaApiFieldMapping>> entry : mappingsByApi.entrySet()) {

            Long apiId = entry.getKey();

            List<AuaApiFieldMapping> apiMappings = entry.getValue();

            if (apiMappings.isEmpty()) {
                continue;
            }

            AuaApiDefinition api = apiMappings.getFirst().getApiDefinition();

            metadataCreationLogger.info(
                    "AUA API definition resolved. apiId={}, apiCode={}, apiName={}, operationType={}, protocol={}",
                    api.getId(),
                    api.getApiCode(),
                    api.getApiName(),
                    api.getOperationType(),
                    api.getProtocol()
            );

            /*
             * Provider.
             */
            Long providerId = apiMappings.getFirst().getProviderId();

            metadataCreationLogger.info(
                    "Resolving AUA provider. providerId={}, apiId={}",
                    providerId,
                    apiId
            );

            AuaProvider provider = auaProviderRepository
                    .findByIdAndActiveTrueAndTenantId(providerId, tenantId)
                    .orElseThrow(() -> new RuntimeException("AUA provider not found: " + providerId));

            metadataCreationLogger.info(
                    "AUA provider resolved. providerId={}, providerName={}",
                    provider.getId(),
                    provider.getProviderName()
            );

            AuaApiConfigurationDTO apiDTO = new AuaApiConfigurationDTO();

            apiDTO.setAuaId(provider.getId());
            apiDTO.setProviderName(provider.getProviderName());

            apiDTO.setApiId(api.getId());
            apiDTO.setApiCode(api.getApiCode());
            apiDTO.setApiName(api.getApiName());
            apiDTO.setOperationType(api.getOperationType());
            apiDTO.setApiVersion(api.getApiVersion());
            apiDTO.setProtocol(api.getProtocol());
            apiDTO.setHttpMethod(api.getHttpMethod());
            apiDTO.setEndpoint(api.getEndpoint());
            apiDTO.setDescription(api.getDescription());
            apiDTO.setXmlNamespace(api.getXmlNamespace());
            apiDTO.setXmlNamespaceVersion(api.getXmlNamespaceVersion());

            /*
             * -----------------------------------------------------
             * Messages
             * -----------------------------------------------------
             */
            List<AuaApiMessage> messages = messageRepository.findByApiDefinitionIdAndActiveTrueAndTenantIdOrderById(api.getId(), tenantId);

            metadataCreationLogger.info(
                    "AUA API messages fetched. apiId={}, messageCount={}",
                    api.getId(),
                    messages == null ? 0 : messages.size()
            );

            List<AuaApiConfigurationDTO.AuaApiMessageDTO> messageDTOs = new ArrayList<>();

            for (AuaApiMessage message : messages) {

                metadataCreationLogger.info(
                        "Processing AUA message. messageId={}, apiId={}, messageType={}, rootElement={}",
                        message.getId(),
                        api.getId(),
                        message.getMessageType(),
                        message.getRootElement()
                );

                AuaApiConfigurationDTO.AuaApiMessageDTO messageDTO = new AuaApiConfigurationDTO.AuaApiMessageDTO();

                messageDTO.setMessageId(message.getId());
                messageDTO.setMessageType(message.getMessageType());
                messageDTO.setRootElement(message.getRootElement());

                /*
                 * Fields belonging to this message.
                 */
                List<AuaApiField> fields = fieldRepository.findByMessageIdAndActiveTrueAndTenantIdOrderByDisplayOrder(message.getId(), tenantId);

                metadataCreationLogger.info(
                        "AUA fields fetched. messageId={}, fieldCount={}",
                        message.getId(),
                        fields == null ? 0 : fields.size()
                );

                List<AuaApiConfigurationDTO.AuaApiMessageDTO.AuaApiFieldDTO> fieldDTOs = new ArrayList<>();

                for (AuaApiField field : fields) {

                    metadataCreationLogger.info(
                            "Processing AUA field. fieldId={}, fieldCode={}, xpath={}, generationType={}, messageId={}, tenantId={}",
                            field.getId(),
                            field.getFieldCode(),
                            field.getXpath(),
                            field.getGenerationType(),
                            message.getId(),
                            tenantId
                    );

                    AuaApiConfigurationDTO.AuaApiMessageDTO.AuaApiFieldDTO fieldDTO = new AuaApiConfigurationDTO.AuaApiMessageDTO.AuaApiFieldDTO();

                    fieldDTO.setFieldId(field.getId());
                    fieldDTO.setFieldCode(field.getFieldCode());
                    fieldDTO.setFieldName(field.getFieldName());
                    fieldDTO.setXpath(field.getXpath());
                    fieldDTO.setDataType(field.getDataType());
                    fieldDTO.setFieldType(field.getFieldType());
                    fieldDTO.setRequired(field.getRequired());
                    fieldDTO.setMultiple(field.getMultiple());
                    fieldDTO.setSensitive(field.getSensitive());
                    fieldDTO.setDisplay(field.getDisplayable());

                    /*
                     * Generation type ALWAYS comes from the field
                     * definition, not from frontend mapping request.
                     */
                    fieldDTO.setTransformation(field.getGenerationType());

                    metadataCreationLogger.debug(
                            "Generation type assigned. fieldCode={}, generationType={}",
                            field.getFieldCode(),
                            fieldDTO.getTransformation()
                    );

                    /*
                     * Find mapping for this field.
                     */
                    Optional<AuaApiFieldMapping> fieldMapping = apiMappings.stream()
                            .filter(mapping ->
                                    mapping.getApiField() != null
                                            && mapping.getApiField().getId().equals(field.getId()))
                            .findFirst();

                    if (fieldMapping.isPresent()) {

                        AuaApiFieldMapping mapping = fieldMapping.get();

                        metadataCreationLogger.info(
                                "Frontend mapping FOUND. fieldId={}, fieldCode={}, sourceType={}, sourcePath={}, defaultValue={}",
                                field.getId(),
                                field.getFieldCode(),
                                mapping.getSourceType(),
                                mapping.getSourcePath(),
                                mapping.getDefaultValue()
                        );

                        fieldDTO.setSourceType(mapping.getSourceType());
                        fieldDTO.setSourcePath(mapping.getSourcePath());
                        fieldDTO.setDefaultValue(mapping.getDefaultValue());
                        fieldDTO.setMappingOrder(field.getDisplayOrder());
                        fieldDTO.setResponseAttributeType(mapping.getResponseAttributeType());
                        fieldDTO.setDesiredResponse(mapping.getDesiredResponse());

                    } else {

                        metadataCreationLogger.info(
                                "Frontend mapping NOT FOUND. fieldId={}, fieldCode={}, generationType={}",
                                field.getId(),
                                field.getFieldCode(),
                                field.getGenerationType()
                        );

                        if (field.getGenerationType() != null && !field.getGenerationType().isBlank()
                                && !AuaMappingTransformation.NONE.name().equalsIgnoreCase(field.getGenerationType())) {
                            fieldDTO.setSourceType(AuaMappingSourceType.DYNAMIC.name());
                            fieldDTO.setSourcePath(field.getFieldCode());
                            fieldDTO.setMappingOrder(field.getDisplayOrder());
                        }
                    }

                    fieldDTOs.add(fieldDTO);
                }

                messageDTO.setFields(fieldDTOs);

                messageDTOs.add(messageDTO);
            }

            apiDTO.setMessages(messageDTOs);

            result.add(apiDTO);
        }

        return result;
    }

	private List<NotificationConfigurationKafkaDTO> buildNotificationConfigurationKafkaDTOs(
			List<NotificationConfiguration> configurations) {

		List<NotificationConfigurationKafkaDTO> result = new ArrayList<>();

		if (configurations == null || configurations.isEmpty()) {
			return result;
		}

		for (NotificationConfiguration configuration : configurations) {

			result.add(buildKafkaDTO(configuration));
		}

		return result;
	}
	
	private NotificationConfigurationKafkaDTO buildKafkaDTO(
	        NotificationConfiguration entity) {

	    NotificationConfigurationKafkaDTO dto =
	            new NotificationConfigurationKafkaDTO();

	    dto.setNotificationId(entity.getNotificationId());
	    dto.setBaseServiceId(entity.getBaseServiceId());
	    dto.setServiceId(entity.getServiceId());
	    dto.setNotificationName(entity.getNotificationName());
	    dto.setChannel(entity.getChannel());
	    dto.setTriggerPointId(entity.getTriggerPointId());
	    dto.setTaskId(entity.getTaskId());
	    dto.setConfigJson(entity.getConfigJson());

	    dto.setCreatedBy(entity.getCreatedBy());
	    dto.setModifiedBy(entity.getModifiedBy());
	    dto.setTenantId(entity.getTenantId());

	    return dto;
	}

    private void updateOfficeLevelIdsWithServiceDeliveryUnit(
            List<OfficeDetailsDTO> officeDetails,
            List<ServiceDeliveryUnitMappingDTO> serviceDeliveryUnitMappings) {
        if (officeDetails == null || officeDetails.isEmpty()
                || serviceDeliveryUnitMappings == null
                || serviceDeliveryUnitMappings.isEmpty()) {
            return;
        }
        for (ServiceDeliveryUnitMappingDTO mapping : serviceDeliveryUnitMappings) {
            Integer deliveryUnitLevelId = mapping.getDeliveryUnitLevelId();
            boolean exists = officeDetails.stream()
                    .anyMatch(officeDetail ->
                            officeDetail.getOfficeLevelIds() != null
                                    && officeDetail.getOfficeLevelIds().contains(deliveryUnitLevelId));
            if (!exists) {
                OfficeDetailsDTO officeDetail = new OfficeDetailsDTO();
                officeDetail.setTaskId("DO");
                Set<Integer> officeLevelIds = new HashSet<>();
                officeLevelIds.add(deliveryUnitLevelId);
                officeDetail.setOfficeLevelIds(officeLevelIds);
                officeDetail.setAllowedOffices(mapping.getOfficeUnits());
                officeDetails.add(officeDetail);
            }
        }
    }

	private List<ServiceDeliveryUnitMappingDTO> buildServiceDeliveryUnitMappingWithUser(
			List<ServiceDeliveryUnitJSON> serviceDeliveryUnits,
			Map<Object, List<OfficeUnitData>> entityLevelOfficeUnitMap) {
		List<ServiceDeliveryUnitMappingDTO> mappings = new ArrayList<>();
		if (serviceDeliveryUnits == null || serviceDeliveryUnits.isEmpty()) {
			return mappings;
		}
		for (ServiceDeliveryUnitJSON serviceDeliveryUnit : serviceDeliveryUnits) {
			ServiceDeliveryUnitMappingDTO mapping = new ServiceDeliveryUnitMappingDTO();
			// Delivery Unit
			mapping.setDeliveryUnitLevelId(
					Integer.parseInt(serviceDeliveryUnit.getServiceDeliveryUnit().getValue().toString()));
			mapping.setDeliveryUnitLevelName(serviceDeliveryUnit.getServiceDeliveryUnit().getLabel());
			// User Role
			Integer userRoleId = Integer.parseInt(serviceDeliveryUnit.getUserRole().getValue().toString());
			mapping.setUserRoleId(userRoleId);
			if (userRoleId == 3) {
				mapping.setUserRole("Designated Officer");
				mapping.setDesignatedOfficerUserLevelId(
						Integer.parseInt(serviceDeliveryUnit.getUserLevel().getEntityLevel().getValue().toString()));
				mapping.setDesignatedOfficerUserLevelName(serviceDeliveryUnit.getUserLevel().getEntityLevel().getLabel());
			}
			mapping.setOfficeUnits(
					getOfficeUnits(serviceDeliveryUnit.getServiceDeliveryUnit(), entityLevelOfficeUnitMap));
			mappings.add(mapping);
		}
		return mappings;
	}

	private List<OfficeUnitData> getOfficeUnits(HierarchyLabelValue hierarchy,
			Map<Object, List<OfficeUnitData>> entityLevelOfficeUnitMap) {
		Entities entity = new Entities();
		entity.setEntity(hierarchy.getEntity());
		entity.setEntityLevel(hierarchy.getEntityLevel());
		entity.setClcDetail(hierarchy.getClcDetail());
		return getOfficeUnits(entity, entityLevelOfficeUnitMap);
	}
        
    private List<RecommendedServiceFormKafkaDTO.Form> getFormIdAndName(List<NodeDTO> nodes) {
        List<RecommendedServiceFormKafkaDTO.Form> formList = new ArrayList<RecommendedServiceFormKafkaDTO.Form>();

        nodes.forEach(node -> formList.add(new RecommendedServiceFormKafkaDTO.Form(
                node.getData().getFormDetail() != null ? node.getData().getFormDetail().getFormId() : null,
                node.getData().getFormDetail() != null ? node.getData().getFormDetail().getParentForm() : null
        )));

        return formList;
    }

    private List<OutputFormatFreezeDTO> getOutputFormatDetails(List<ServiceOutputFormat> serviceOutputFormatList) {

        List<OutputFormatFreezeDTO> outputFormatDetails = new ArrayList<>();

        for (ServiceOutputFormat serviceOutputFormat : serviceOutputFormatList) {

            if (serviceOutputFormat.getOutputFormatJson() == null) {
                continue;
            }

            ServiceOutputFormatDTO outputFormatJson =
                    serviceOutputFormat.getOutputFormatJson();

            OutputFormatFreezeDTO dto = new OutputFormatFreezeDTO();

            dto.setId(serviceOutputFormat.getId());
            dto.setLayout(outputFormatJson.getLayout());
            dto.setPageSize(outputFormatJson.getPageSize());
            dto.setUsedAttr(outputFormatJson.getUsedAttr());
            dto.setTemplateName(outputFormatJson.getTemplateName());
            dto.setDocumentType(outputFormatJson.getDocumentType());
            dto.setTemplateBodySrc(outputFormatJson.getTemplateBodySrc());
            dto.setWaterMark(outputFormatJson.getWaterMark());

            outputFormatDetails.add(dto);
        }

        return outputFormatDetails;
    }

    private List<ExternalAttributeMappingDTO> buildExternalSystem(List<NodeDTO> nodes) {

        List<ExternalAttributeMappingDTO> result = new ArrayList<>();

        for (NodeDTO node : nodes) {

            if (node == null || node.getData() == null
                    || node.getData().getExternalSystemDetail() == null
                    || node.getData().getExternalSystemDetail().isEmpty()) {
                continue;
            }

            for (ExternalSystemDetail externalDetail : node.getData().getExternalSystemDetail()) {

                ExternalAttributeMappingDTO dto = new ExternalAttributeMappingDTO();
                dto.setNodeId(node.getId());
                dto.setExternalSystemRegId(externalDetail.getExternalSystemRegId());

                if (externalDetail.getMappingData() != null
                        && !externalDetail.getMappingData().isEmpty()) {

                    List<AttrMappingDetails> mappingDetails = externalDetail.getMappingData()
                            .stream()
                            .map(mapping -> {
                                AttrMappingDetails attr = new AttrMappingDetails();
                                attr.setNodeRef(mapping.getNodeReference());
                                attr.setAttributeId(mapping.getUniqueId());
                                return attr;
                            })
                            .collect(Collectors.toList());

                    dto.setAttrMappingDetails(mappingDetails);
                }

                result.add(dto);
            }
        }

        return result;
    }
    private Set<String> getAttributeUsedInMvel(List<MvelFunction> mvelFunctionList) {
        Set<String> usedMvelAttribute=new HashSet<String>();
        if (mvelFunctionList == null || mvelFunctionList.isEmpty()) {
            return usedMvelAttribute;
        }
        for (MvelFunction mvelFunction : mvelFunctionList) {
            MvelFunctionDTO dto = mvelFunction.getMvelFunctionJson();
            if (dto.getParameters() != null && !dto.getParameters().isEmpty()) {
                for (MvelFunctionDTO.FunctionParameter p : dto.getParameters()) {
                    if(p.getParamType().equals(ApplicationConstants.ATTRIBUTE))
                        usedMvelAttribute.add(p.getParamId());
                }
            }
        }
        return usedMvelAttribute;
    }
    private List<MvelDetailsDTO> buildMvelDetails(List<MvelFunction> mvelFunctionList) {
        List<MvelDetailsDTO> mvelDetailList = new ArrayList<>();

        if (mvelFunctionList == null || mvelFunctionList.isEmpty()) {
            return mvelDetailList;
        }
        MvelFunctionEvent mvelFnEvent = new MvelFunctionEvent();
        List<MvelFunctionEvent.FunctionPayload> functions=new ArrayList<>();
        mvelFnEvent.setEventType("Freeze");

        if (!mvelFunctionList.isEmpty()) {
            mvelFnEvent.setServiceId(mvelFunctionList.get(0).getServiceId());
            mvelFnEvent.setVersionNo(mvelFunctionList.get(0).getVersionNo());
            mvelFnEvent.setMinorVersionNo(mvelFunctionList.get(0).getMinorVersionNo());
        }
        for (MvelFunction mvelFunction : mvelFunctionList) {
            MvelDetailsDTO mvelDetails = new MvelDetailsDTO();
            MvelFunctionDTO dto = mvelFunction.getMvelFunctionJson();
            MvelFunctionEvent.FunctionPayload fnPayload = new MvelFunctionEvent.FunctionPayload();
            fnPayload.setFunctionId(mvelFunction.getId());
            fnPayload.setFunctionName(dto.getFunctionName());
            fnPayload.setFunctionBody(dto.getFunctionBody());

            if (dto.getParameters() != null && !dto.getParameters().isEmpty()) {

                List<MvelFunctionEvent.FunctionPayload.ParameterPayload> paramList = new ArrayList<>();

                for (MvelFunctionDTO.FunctionParameter p : dto.getParameters()) {

                    MvelFunctionEvent.FunctionPayload.ParameterPayload param =
                            new MvelFunctionEvent.FunctionPayload.ParameterPayload();

                    param.setParamId(p.getParamId());
                    param.setParamType(p.getParamType());
                    paramList.add(param);
                }

                fnPayload.setParameters(paramList);
            }
            TriggerPoint triggerPoint = mvelFunction.getMvelFunctionJson().getTriggerPoint();

            if (triggerPoint.getValue() == null || triggerPoint.getNodeId() == null) {
                continue;
            }
            mvelDetails.setLabel(triggerPoint.getLabel());
            mvelDetails.setValue(triggerPoint.getValue());
            mvelDetails.setNodeId(triggerPoint.getNodeId());
            mvelDetails.setMvelId(mvelFunction.getId());
            mvelDetailList.add(mvelDetails);
            functions.add(fnPayload);
        }
        mvelFnEvent.setFunctions(functions);
        mvelKafkaProducer.publishFreezeEvent(mvelFnEvent);
        return mvelDetailList;
    }
    private List<WorkFlowDataDTO> buildWorkFlowData(List<NodeDTO> nodes) {
        List<WorkFlowDataDTO> workFlowDataList = new ArrayList<>();

        for (NodeDTO node : nodes) {
            if (!ApplicationConstants.GATEWAY_NODE_TYPE.equals(node.getType())) {
                WorkFlowDataDTO dto = new WorkFlowDataDTO();
                dto.setTaskId(node.getId());

                List<WorkFlowAction> actionList = new ArrayList<>();
                Map<String, Action> workflow = node.getData().getWorkflow();
                for (Map.Entry<String, Action> entry : workflow.entrySet()) {

                    Action action = entry.getValue();
                    if (Boolean.TRUE.equals(action.getEnabled())) {

                        WorkFlowAction actionDTO = new WorkFlowAction();
                        actionDTO.setActionKey(entry.getKey().split("_")[1]);
                        actionDTO.setActionLabel(action.getActionFormLabel());
                        actionDTO.setTrackLabel(action.getTrackLabel());
                        actionDTO.setTrackLabelOfficial(action.getHistoryLabel());
                        if (action.getRemarks() != null) {
                            actionDTO.setRemarksRequired(action.getRemarks().getMandatory());
                        }
                        actionDTO.setLogicalClosure(action.getLogicalClosure());
                        actionDTO.setCompleteClosure(action.getCompleteClosure());
                        actionList.add(actionDTO);
                    }
                }

                dto.setAllowedAction(actionList);
                workFlowDataList.add(dto);
            }
        }
        return workFlowDataList;
    }
    private Set<String> getAttributeListUsedInService(List<ServiceOutputFormat> serviceOutputFormatList) {
        Set<String> usedAttributeList= new HashSet<String>();
        for(ServiceOutputFormat serviceOutputFormat:serviceOutputFormatList) {
            if(serviceOutputFormat.getOutputFormatJson()!=null) {
                List<String> attributeIds = serviceOutputFormat.getOutputFormatJson().getUsedAttr().stream().map(attr -> attr.replaceFirst("^\\$attr_", "")).toList();
                usedAttributeList.addAll(attributeIds);
            }
        }
        return usedAttributeList;
    }
    private List<OfficeDetailsDTO> buildOfficeData(List<NodeDTO> nodes, Map<Object, List<OfficeDetailsDTO.OfficeUnitData>> entityLevelOfficeUnitMap) {
        List<OfficeDetailsDTO> officeList = new ArrayList<>();

        for (NodeDTO node : nodes) {
            if (!ApplicationConstants.GATEWAY_NODE_TYPE.equals(node.getType())) {
                OfficeDetailsDTO dto = new OfficeDetailsDTO();
                List<OfficeUnitData> officeUnitData=new ArrayList<>();
                Set<Integer> officeLevelIds=new HashSet<>();
                dto.setTaskId(node.getId());
                if (node.getData().getEntities() != null && !node.getData().getEntities().isEmpty()) {
                    for(Entities dept:node.getData().getEntities()) {
                        officeLevelIds.add(Integer.parseInt(dept.getEntityLevel().getValue().toString()));
                        officeUnitData.addAll(getOfficeUnits(dept, entityLevelOfficeUnitMap));
                    }
                    dto.setAllowedOffices(officeUnitData);
                    dto.setOfficeLevelIds(officeLevelIds);
                }
                officeList.add(dto);
            }
        }
        return officeList;
    }
    private ServiceLog validateAndGetServiceLog(Integer serviceId) {
        ServiceLog serviceLog = serviceLogRepository.findByServiceId(serviceId);
        if (serviceLog == null) {
            throw new RuntimeException("Invalid Service");
        }
        return serviceLog;
    }

    private ServiceProcessFlow validateAndGetProcessFlow(Integer serviceId, Long userId) {
        ServiceProcessFlow flow = processFlowRepository.findByServiceIdAndUserId(serviceId, userId);

        if (flow == null || flow.getProcessFlowJson() == null) {
            throw new RuntimeException("Process flow does not exist for this service and user.");
        }
        return flow;
    }

    private NodeDTO getSubmissionNode(List<NodeDTO> nodes) {
        return nodes.stream()
                .filter(n -> ApplicationConstants.SUBMISSION_NODE_TYPE.equals(n.getType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Submission node not found in process flow"));
    }
    
    private void addAssociatedTaskOfficeDetails(List<OfficeDetailsDTO> officeDetails, AssociatedTaskDTO task,
			Map<Object, List<OfficeDetailsDTO.OfficeUnitData>> entityLevelOfficeUnitMap) {

		if (task == null || task.getOfficialIntimation() == null || task.getOfficialIntimation().getEntities() == null
				|| task.getOfficialIntimation().getEntities().isEmpty()) {
			return;
		}
		
		OfficeDetailsDTO officeDTO = null;

		for (OfficeDetailsDTO dto : officeDetails) {
			if (Objects.equals(dto.getTaskId(), task.getId())) {
				officeDTO = dto;
				break;
			}
		}

		if (officeDTO == null) {
			officeDTO = new OfficeDetailsDTO();
			officeDTO.setTaskId(task.getId());
			officeDTO.setAllowedOffices(new ArrayList<>());
			officeDTO.setOfficeLevelIds(new HashSet<>());
			officeDetails.add(officeDTO);
		}

		List<OfficeUnitData> offices = officeDTO.getAllowedOffices();
		Set<Integer> officeLevels = officeDTO.getOfficeLevelIds();

		for (Entities entity : task.getOfficialIntimation().getEntities()) {

			officeLevels.add(Integer.parseInt(entity.getEntityLevel().getValue().toString()));

			List<OfficeUnitData> unitData = getOfficeUnits(entity, entityLevelOfficeUnitMap);

			for (OfficeUnitData unit : unitData) {
				boolean exists = false;

				for (OfficeUnitData existing : offices) {
					if (Objects.equals(existing.getOrgUnitCode(), unit.getOrgUnitCode())) {
						exists = true;
						break;
					}
				}

				if (!exists) {
					offices.add(unit);
				}
			}
		}
	}

    private ServiceProcessFlowDTO buildProcessFlowMap(Integer serviceId, List<NodeDTO> nodes, List<EdgeDTO> edges, List<OfficeDetailsDTO> officeDetails, Map<String, String> taskFormMapping) {

        Map<String, List<String>> edgeMap = edges.stream()
                .collect(Collectors.groupingBy(
                        EdgeDTO::getSource,
                        Collectors.mapping(EdgeDTO::getTarget, Collectors.toList())
                ));

        Map<String, NodeDTO> nodeMap = nodes.stream()
                .collect(Collectors.toMap(NodeDTO::getId, Function.identity()));

        ServiceProcessFlowDTO serviceProcessFlowDTO = new ServiceProcessFlowDTO();
        serviceProcessFlowDTO.setServiceId(serviceId);

        List<ServiceProcessFlowDTO.Data> dataList = new ArrayList<>();

        for (NodeDTO node : nodes) {
            List<OfficeDetailsDTO> nodeOfficeDetails = officeDetails.stream().filter(off -> off.getTaskId().equals(node.getId())).collect(Collectors.toList());
            ServiceProcessFlowDTO.Data data = new ServiceProcessFlowDTO.Data();

            ServiceProcessFlowDTO.Data.Nodes dtoNode = new ServiceProcessFlowDTO.Data.Nodes();
            dtoNode.setId(node.getId());
            dtoNode.setType(node.getType());
            dtoNode.setName(node.getData() != null ? node.getData().getName() : null);
            dtoNode.setBehaviour(node.getData() != null && node.getData().getBehaviour() != null ? (String) node.getData().getBehaviour().getValue():null);
            TaskType taskType = TaskType.resolve(node.getData());
            dtoNode.setTaskType(taskType != null ? taskType.getType() : null);
            data.setNode(dtoNode);

            List<String> targetNodeIds = edgeMap.getOrDefault(node.getId(), Collections.emptyList());

            List<ServiceProcessFlowDTO.Data.MappedTask> mappedTasks = targetNodeIds.stream()
                    .map(targetId -> {
                        ServiceProcessFlowDTO.Data.MappedTask mt = new ServiceProcessFlowDTO.Data.MappedTask();

                        NodeDTO targetNode = nodeMap.get(targetId);
                        if (targetNode != null) {
                            ServiceProcessFlowDTO.Data.Nodes mappedNode = new ServiceProcessFlowDTO.Data.Nodes();
                            mappedNode.setId(targetNode.getId());
                            mappedNode.setType(targetNode.getType());
                            mappedNode.setName(targetNode.getData() != null ? targetNode.getData().getName() : null);
                            mappedNode.setBehaviour(targetNode.getData() != null && targetNode.getData().getBehaviour() != null? (String) targetNode.getData().getBehaviour().getValue():null);
                            mappedNode.setFormId(taskFormMapping.get(targetNode.getId()));
                            TaskType mappedTaskType = TaskType.resolve(targetNode.getData());
                            mappedNode.setTaskType(mappedTaskType != null ? mappedTaskType.getType() : null);
                            mt.setNode(mappedNode);
                        }

                        return mt;
                    })
                    .collect(Collectors.toList());

            data.setMappedTasks(mappedTasks);

            if (TYPE_TASK.equalsIgnoreCase(node.getType())) {
                data.setWorkflowElementData(
                        buildWorkflowElementData(
                                node,
                                mappedTasks,
                                edgeMap,
                                nodeMap,
                                officeDetails
                        )
                );
            }


            if(!nodeOfficeDetails.isEmpty()) {
                data.setAllowedOffices(nodeOfficeDetails.getFirst().getAllowedOffices());
            }
            dataList.add(data);
        }

        serviceProcessFlowDTO.setData(dataList);
        serviceProcessFlowDTO.setTaskRelation(buildTaskRelationMap(nodes, edges));
        return serviceProcessFlowDTO;
    }
    private Map<String, TaskRelationDTO> buildTaskRelationMap(List<NodeDTO> nodes, List<EdgeDTO> edges) {

        Map<String, NodeDTO> nodeMap = nodes.stream()
                .collect(Collectors.toMap(NodeDTO::getId, Function.identity()));

        Map<String, List<String>> outgoingMap = edges.stream()
                .collect(Collectors.groupingBy(
                        EdgeDTO::getSource,
                        Collectors.mapping(EdgeDTO::getTarget, Collectors.toList())
                ));

        Map<String, List<String>> incomingMap = edges.stream()
                .collect(Collectors.groupingBy(
                        EdgeDTO::getTarget,
                        Collectors.mapping(EdgeDTO::getSource, Collectors.toList())
                ));

        Map<String, TaskRelationDTO> relationMap = new LinkedHashMap<>();

        for (NodeDTO node : nodes) {

            TaskRelationDTO dto = new TaskRelationDTO();

            Set<String> nextTask = new LinkedHashSet<>();
            collectNextTasks(node.getId(),outgoingMap,nodeMap,new HashSet<>(),nextTask);

            Set<String> previousTask = new LinkedHashSet<>();
            collectPreviousTasks(node.getId(),incomingMap,nodeMap,new HashSet<>(),previousTask);

            dto.setNextTask(new ArrayList<>(nextTask));
            dto.setPreviousTask(new ArrayList<>(previousTask));

            relationMap.put(node.getId(), dto);
        }

        return relationMap;
    }
	
	private void collectNextTasks(String nodeId, Map<String, List<String>> edgeMap, Map<String, NodeDTO> nodeMap,
			Set<String> visited, Set<String> result) {

		if (!visited.add(nodeId)) {
			return;
		}

		List<String> targets = edgeMap.get(nodeId);
		if(targets!=null && !targets.isEmpty()) {
			for (String target : targets) {
	
				NodeDTO node = nodeMap.get(target);
	
				if (node == null) {
					continue;
				}
	
				if (ApplicationConstants.GATEWAY_NODE_TYPE.equals(node.getType())) {
					collectNextTasks(target, edgeMap, nodeMap, visited, result);
				} else {
					result.add(target);
				}
			}
		}
	}

	private void collectPreviousTasks(String nodeId, Map<String, List<String>> incomingMap,
			Map<String, NodeDTO> nodeMap, Set<String> visited, Set<String> result) {

		if (!visited.add(nodeId)) {
			return;
		}

		List<String> sources = incomingMap.get(nodeId);
		if(sources!=null && !sources.isEmpty()) {
			for (String source : sources) {
	
				NodeDTO node = nodeMap.get(source);
	
				if (node == null) {
					continue;
				}
	
				if (ApplicationConstants.GATEWAY_NODE_TYPE.equals(node.getType())) {
					collectPreviousTasks(source, incomingMap, nodeMap, visited, result);
				} else {
					result.add(source);
				}
			}
		}
	}

    private ServiceProcessFlowDTO.Data.WorkflowElementData buildWorkflowElementData(
            NodeDTO currentNode,
            List<ServiceProcessFlowDTO.Data.MappedTask> mappedTasks,
            Map<String, List<String>> edgeMap,
            Map<String, NodeDTO> nodeMap,
            List<OfficeDetailsDTO> officeDetails) {

        ServiceProcessFlowDTO.Data.WorkflowElementData workflowElementData = new ServiceProcessFlowDTO.Data.WorkflowElementData();

        // ---------------- Action Attributes ----------------

        List<ServiceProcessFlowDTO.Data.ActionAttribute> actionAttributes = new ArrayList<>();

        if (currentNode.getData() != null && currentNode.getData().getWorkflow() != null) {

            currentNode.getData().getWorkflow().forEach((actionKey, workflow) -> {

                if (Boolean.TRUE.equals(workflow.getEnabled())) {

                    ServiceProcessFlowDTO.Data.ActionAttribute action =
                            new ServiceProcessFlowDTO.Data.ActionAttribute();

                    action.setKey(actionKey.replace("action_", ""));
                    action.setLabel(workflow.getActionFormLabel());
                    action.setTrackLabel(workflow.getTrackLabel());
                    action.setLogicalClosure(workflow.getLogicalClosure());
                    action.setCompleteClosure(workflow.getCompleteClosure());

                    actionAttributes.add(action);
                }
            });
        }

        workflowElementData.setActionAttribute(actionAttributes);

        if (!mappedTasks.isEmpty()) {

            ServiceProcessFlowDTO.Data.Nodes nextNode = mappedTasks.getFirst().getNode();

            // ---------------- Gateway ----------------

            if (TYPE_GATEWAY.equalsIgnoreCase(nextNode.getType())) {

                String selectionType = null;

                if (GATEWAY_BEHAVIOUR_EXCLUSIVE_DIVERGENT.equalsIgnoreCase(nextNode.getBehaviour())) {
                    selectionType = "RADIO";
                } else if (GATEWAY_BEHAVIOUR_INCLUSIVE_DIVERGENT.equalsIgnoreCase(nextNode.getBehaviour())) {
                    selectionType = "CHECKBOX";
                }

                if (!isNull(selectionType)) {

                    ServiceProcessFlowDTO.Data.TaskAttribute taskAttribute =
                            new ServiceProcessFlowDTO.Data.TaskAttribute();

                    List<ServiceProcessFlowDTO.Data.TaskNode> taskNodes = new ArrayList<>();

                    ServiceProcessFlowDTO.Data.UserAttribute userAttribute =
                            new ServiceProcessFlowDTO.Data.UserAttribute();

                    List<ServiceProcessFlowDTO.Data.UserNode> userNodes = new ArrayList<>();

                    taskAttribute.setGatewayType(nextNode.getBehaviour());

                    List<String> gatewayTargets = edgeMap.getOrDefault(nextNode.getId(), Collections.emptyList());

                    for (String targetId : gatewayTargets) {

                        NodeDTO taskNode = nodeMap.get(targetId);

                        if (taskNode == null) {
                            continue;
                        }

                        boolean showTask = false;
                        boolean showUser = false;

                        if (taskNode.getData() != null
                                && taskNode.getData().getAdvanced() != null
                                && taskNode.getData().getAdvanced().getDisplayUserTask() != null
                                && taskNode.getData().getAdvanced().getDisplayUserTask().getData() != null) {

                            showTask = taskNode.getData()
                                    .getAdvanced()
                                    .getDisplayUserTask()
                                    .getData()
                                    .isTaskVisibilityOnPreviousTask();

                            showUser = taskNode.getData()
                                    .getAdvanced()
                                    .getDisplayUserTask()
                                    .getData()
                                    .isUserVisibilityOnPreviousTask();
                        }

                        // ---------------- Task ----------------

                        ServiceProcessFlowDTO.Data.TaskNode task = new ServiceProcessFlowDTO.Data.TaskNode();

                        task.setTaskId(taskNode.getId());
                        task.setTaskName(taskNode.getData().getName());
                        task.setShowSelection(showTask);

                        taskNodes.add(task);

                        // ---------------- User ----------------

                        ServiceProcessFlowDTO.Data.UserNode user = new ServiceProcessFlowDTO.Data.UserNode();

                        user.setTaskId(taskNode.getId());
                        user.setShowSelection(showUser);

                        userNodes.add(user);
                    }

                    taskAttribute.setSelectionType(selectionType);
                    taskAttribute.setTaskNodes(taskNodes);

                    userAttribute.setSelectionType("CHECKBOX");
                    userAttribute.setUserNodes(userNodes);

                    workflowElementData.setTaskAttribute(taskAttribute);
                    workflowElementData.setUserAttribute(userAttribute);
                }
            }

            // ---------------- Direct Task ----------------
            // No TaskAttribute required.
            // But UserAttribute can still be configured for a direct task.

            ServiceProcessFlowDTO.Data.UserAttribute userAttribute = new ServiceProcessFlowDTO.Data.UserAttribute();

            List<ServiceProcessFlowDTO.Data.UserNode> userNodes = new ArrayList<>();

            if (!TYPE_GATEWAY.equalsIgnoreCase(nextNode.getType())) {

                OfficeDetailsDTO officeDetail = officeDetails.stream()
                        .filter(o -> o.getTaskId() != null && o.getTaskId().equals(nextNode.getId()))
                        .findFirst()
                        .orElse(null);

                if (officeDetail != null && officeDetail.getAllowedOffices() != null && !officeDetail.getAllowedOffices().isEmpty()) {

                    for (OfficeDetailsDTO.OfficeUnitData office : officeDetail.getAllowedOffices()) {

                        ServiceProcessFlowDTO.Data.UserNode user = new ServiceProcessFlowDTO.Data.UserNode();

                        user.setTaskId(nextNode.getId());
                        user.setLocationId(office.getOrgUnitCode() != null ? office.getOrgUnitCode().longValue() : null);
                        user.setLocationName(office.getOrgUnitName());
                        user.setShowSelection(Boolean.TRUE);

                        userNodes.add(user);
                    }
                }
            }

            if (!userNodes.isEmpty()) {
                userAttribute.setSelectionType("CHECKBOX");
                userAttribute.setUserNodes(userNodes);
                workflowElementData.setUserAttribute(userAttribute);
            }
        }

        boolean priority = false;
        if (currentNode.getData() != null
                && currentNode.getData().getAdvanced() != null
                && currentNode.getData().getAdvanced().getOfficial() != null) {
            priority = currentNode.getData().getAdvanced().getOfficial().isPriority();
        }
        workflowElementData.setPriorityEnabled(priority);
        
        return workflowElementData;
    }

    private List<DocumentGenerationDetails> buildDocumentGenerationDetails(List<NodeDTO> nodes) {

        metadataCreationLogger.info("========== Building Document Generation Metadata ==========");

        List<DocumentGenerationDetails> result = new ArrayList<>();

        if (nodes == null || nodes.isEmpty()) {
            metadataCreationLogger.info("No nodes found.");
            return result;
        }

        metadataCreationLogger.info("Total Nodes : {}", nodes.size());

        for (NodeDTO node : nodes) {

            metadataCreationLogger.info("---------------------------------------------");
            metadataCreationLogger.info("Processing Node : {} ({})", node.getId(), node.getType());

            // Only submission & task participate
            if (!(ApplicationConstants.TYPE_TASK.equalsIgnoreCase(node.getType())
                    || ApplicationConstants.SUBMISSION_NODE_TYPE.equalsIgnoreCase(node.getType()))) {

                metadataCreationLogger.info("Skipping node because type is {}", node.getType());
                continue;
            }

            DocumentGenerationDetails dto = new DocumentGenerationDetails();
            dto.setTaskId(node.getId());

            List<DocMappingDTO> mappings = new ArrayList<>();

            if (node.getData() == null) {

                metadataCreationLogger.info("Node data is null.");
                dto.setDocumentMapping(mappings);
                result.add(dto);
                continue;
            }

            if (node.getData().getDocumentMapping() == null
                    || node.getData().getDocumentMapping().isEmpty()) {

                metadataCreationLogger.info("No document mapping configured for {}", node.getId());

                dto.setDocumentMapping(mappings);
                result.add(dto);
                continue;
            }

            metadataCreationLogger.info("Found {} document mappings",
                    node.getData().getDocumentMapping().size());

            for (DocMappingDTO source : node.getData().getDocumentMapping()) {

                metadataCreationLogger.info("Document : {}", source.getDocumentName());

                DocMappingDTO target = new DocMappingDTO();

                target.setReferenceId(source.getReferenceId());
                target.setDocumentName(source.getDocumentName());
                target.setDocumentSource(source.getDocumentSource());

                //-----------------------------
                // Digital Signature
                //-----------------------------

                if (source.getDigitalSignatureRequired() != null) {

                    String ds = source.getDigitalSignatureRequired().trim().toLowerCase();

                    if ("yes".equals(ds)) {
                        target.setDigitalSignatureRequired("true");
                    } else if ("no".equals(ds)) {
                        target.setDigitalSignatureRequired("false");
                    } else {
                        target.setDigitalSignatureRequired("false");
                    }
                }

                target.setViewPermission(source.getViewPermission());
                target.setOfficialViewTasks(source.getOfficialViewTasks());

                target.setIsFileUploadOptional(source.getIsFileUploadOptional());
                target.setIsDraftDocumentRequired(source.getIsDraftDocumentRequired());

                target.setMergeMode(source.getMergeMode());

                //-----------------------------
                // Actions
                //-----------------------------

                if (source.getAction() != null) {

                    List<DocMappingDTO.LabelValueDTO> actions = getLabelValueDTOS(source);

                    target.setAction(actions);

                    metadataCreationLogger.info("Mapped {} actions", actions.size());
                }

                //-----------------------------
                // Linked Task
                //-----------------------------

                if (source.getLinkDocumentFromTask() != null) {

                    DocMappingDTO.LabelValueDTO copy = new DocMappingDTO.LabelValueDTO();

                    copy.setLabel(source.getLinkDocumentFromTask().getLabel());
                    copy.setValue(source.getLinkDocumentFromTask().getValue());

                    target.setLinkDocumentFromTask(copy);
                }

                //-----------------------------
                // Linked Document
                //-----------------------------

                if (source.getLinkedDocument() != null) {

                    DocMappingDTO.LabelValueDTO copy = new DocMappingDTO.LabelValueDTO();

                    copy.setLabel(source.getLinkedDocument().getLabel());
                    copy.setValue(source.getLinkedDocument().getValue());

                    target.setLinkedDocument(copy);
                }

                //-----------------------------
                // System Generated
                //-----------------------------

                if (source.getSystemGeneratedDocument() != null) {

                    DocMappingDTO.SystemGeneratedDocumentDTO copy = new DocMappingDTO.SystemGeneratedDocumentDTO();

                    copy.setLabel(source.getSystemGeneratedDocument().getLabel());
                    copy.setValue(source.getSystemGeneratedDocument().getValue());

                    target.setSystemGeneratedDocument(copy);
                }

                //-----------------------------
                // File Types
                //-----------------------------

                if (source.getFileType() != null) {

                    List<DocMappingDTO.LabelValueDTO> fileTypes = getValueDTOS(source);
                    target.setFileType(fileTypes);
                }

                //-----------------------------
                // Merge Order
                //-----------------------------

                if (source.getMergeOrder() != null) {

                    List<DocMappingDTO.MergeOrderDTO> mergeOrders = getMergeOrderDTOS(source);
                    target.setMergeOrder(mergeOrders);
                }

                mappings.add(target);
            }

            dto.setDocumentMapping(mappings);

            result.add(dto);

            metadataCreationLogger.info("Added metadata for task {} with {} documents",
                    node.getId(),
                    mappings.size());
        }

        metadataCreationLogger.info("============================================");
        metadataCreationLogger.info("Total Document Generation Entries : {}", result.size());

        return result;
    }

    private static List<DocMappingDTO.MergeOrderDTO> getMergeOrderDTOS(DocMappingDTO source) {
        List<DocMappingDTO.MergeOrderDTO> mergeOrders = new ArrayList<>();

        for (DocMappingDTO.MergeOrderDTO order : source.getMergeOrder()) {

            DocMappingDTO.MergeOrderDTO copy = new DocMappingDTO.MergeOrderDTO();

            copy.setId(order.getId());
            copy.setLabel(order.getLabel());
            copy.setSortOrder(order.getSortOrder());

            mergeOrders.add(copy);
        }
        return mergeOrders;
    }

    private static List<DocMappingDTO.LabelValueDTO> getValueDTOS(DocMappingDTO source) {
        List<DocMappingDTO.LabelValueDTO> fileTypes = new ArrayList<>();

        for (DocMappingDTO.LabelValueDTO file : source.getFileType()) {

            DocMappingDTO.LabelValueDTO copy = new DocMappingDTO.LabelValueDTO();

            copy.setLabel(file.getLabel());
            copy.setValue(file.getValue());

            fileTypes.add(copy);
        }
        return fileTypes;
    }

    private static List<DocMappingDTO.LabelValueDTO> getLabelValueDTOS(DocMappingDTO source) {
        List<DocMappingDTO.LabelValueDTO> actions = new ArrayList<>();

        for (DocMappingDTO.LabelValueDTO action : source.getAction()) {

            DocMappingDTO.LabelValueDTO copy = new DocMappingDTO.LabelValueDTO();

            copy.setLabel(action.getLabel());
            copy.setValue(action.getValue().replace("action_", ""));

            actions.add(copy);
        }
        return actions;
    }

    private List<ServiceProcessFlowDTO.AssociatedActivity> buildAssociatedActivities(
            List<NodeDTO> nodes,
            List<AssociatedTaskDTO> associatedTasks,
            List<OfficeDetailsDTO> officeDetails,
            Map<Object, List<OfficeDetailsDTO.OfficeUnitData>> entityLevelOfficeUnitMap) {

        if (nodes == null || associatedTasks == null) {
            return Collections.emptyList();
        }

        Map<String, AssociatedTaskDTO> taskMap = associatedTasks.stream()
                .collect(Collectors.toMap(AssociatedTaskDTO::getId, Function.identity()));

        List<ServiceProcessFlowDTO.AssociatedActivity> activityList = new ArrayList<>();

        for (NodeDTO node : nodes) {
            if (node.getData() == null || node.getData().getAssociatedTasks() == null) {
                continue;
            }
			for (AssociatedTaskReferenceDTO ref : node.getData().getAssociatedTasks()) {
				AssociatedTaskDTO task = taskMap.get(ref.getAssociatedTaskId());
				if (task == null) {
					continue;
				}
				ServiceProcessFlowDTO.AssociatedActivity activity = new ServiceProcessFlowDTO.AssociatedActivity();
				activity.setId(task.getId());

				activity.setType(
						task.getAssociatedTaskType() != null ? task.getAssociatedTaskType().getValue().toString()
								: null);
				activity.setSourceTaskId(node.getId());
				activity.setTriggerPoint(ref.getTriggerPoint());
				activity.setTriggerOnAction(ref.getTriggerOnAction());
				activity.setExecutionTasks(ref.getExecutionTasks());
				activity.setFormDetail(ref.getFormDetail());

				WebserviceDetails master = task.getWebServiceDetails();

				if (master != null) {
				    WebserviceDetails details = new WebserviceDetails();

				    details.setWsCall(master.getWsCall());
				    details.setEntities(master.getEntities());
				    details.setWsSelected(master.getWsSelected());
				    details.setApplicationStatus(master.getApplicationStatus());

				    // node-specific
				    details.setDefinition(ref.getApiDefinition());

				    activity.setWebServiceDetails(details);
				}
			
				if (task.getOfficialIntimation() != null) {

					ServiceProcessFlowDTO.OfficialIntimation official = new ServiceProcessFlowDTO.OfficialIntimation();

					official.setAllowApplicationView(task.getOfficialIntimation().getAllowApplicationView());

					official.setAllowHistoryView(task.getOfficialIntimation().getAllowHistoryView());

					official.setAutoClear(task.getOfficialIntimation().getAutoClear());

					List<OfficeUnitData> offices = new ArrayList<>();

					for (Entities entity : task.getOfficialIntimation().getEntities()) {
						offices.addAll(getOfficeUnits(entity, entityLevelOfficeUnitMap));
					}

					official.setAllowedOffices(offices);

					activity.setOfficialIntimation(official);
				}

				addAssociatedTaskOfficeDetails(officeDetails, task, entityLevelOfficeUnitMap);

				activityList.add(activity);
			}
        }

        return activityList;
    }
    private List<OfficeUnitData> getOfficeUnits(Entities entity,Map<Object, List<OfficeDetailsDTO.OfficeUnitData>> entityLevelOfficeUnitMap) {

        Object levelId = entity.getEntityLevel().getValue();

        List<OfficeUnitData> officeUnits = entityLevelOfficeUnitMap.get(levelId);
        if (officeUnits != null && !officeUnits.isEmpty()) {
            return officeUnits;
        }

        LevelEntityRequestDTO request = new LevelEntityRequestDTO();
        request.setLevelCode(Integer.parseInt(levelId.toString()));
        request.setEntityCode(Integer.parseInt(entity.getEntity().getValue().toString()));
        request.setClc(entity.getClcDetail() != null ? Long.parseLong(entity.getClcDetail().getValue().toString()) : null);
        request.setParentUnitCode(null);
        request.setUnitId(null);
        request.setLocationCode(null);

        List<LevelWiseUnitsDTO> units = lgdFeignClient.getLevelWiseUnits(request);

        officeUnits = new ArrayList<>();

        if (units != null) {
            for (LevelWiseUnitsDTO unit : units) {

                OfficeUnitData office = new OfficeUnitData();
                office.setOrgUnitCode(unit.getEntityUnitCode().intValue());
                office.setOrgUnitName(unit.getEntityUnitName());

                officeUnits.add(office);
            }
        }
        entityLevelOfficeUnitMap.put(levelId, officeUnits);

        return officeUnits;
    }
    private Map<String, String> buildTaskFormMapping(List<NodeDTO> nodes) {
        Map<String, String> mapping = new HashMap<>();
        for (NodeDTO node : nodes) {
            if (node.getData().getFormDetail() != null) {
                mapping.put(node.getId(), node.getData().getFormDetail().getFormId());
            }
            if (node.getData().getAssociatedTasks() != null) {
                for (AssociatedTaskReferenceDTO associatedTask : node.getData().getAssociatedTasks()) {
                    if (associatedTask.getFormDetail() != null) {
                        mapping.put(associatedTask.getAssociatedTaskId(),associatedTask.getFormDetail().getFormId());
                    }
                }
            }
        }
        return mapping;
    }

    private List<ActivityMapDTO> buildActivityFlow(List<NodeDTO> nodes,List<NotificationConfiguration> notificationConfigurations) {

		List<ActivityMapDTO> activityFlowList = new ArrayList<>();

		for (NodeDTO node : nodes) {

			if (ApplicationConstants.GATEWAY_NODE_TYPE.equals(node.getType())) {
				continue;
			}

			List<ActivityMapDTO.ActivityData> activityDataList = new ArrayList<>();

			List<NotificationConfiguration> formSubmissionNotifications = new ArrayList<>();

			List<NotificationConfiguration> taskCompletionNotifications = new ArrayList<>();

			if (notificationConfigurations != null) {

				for (NotificationConfiguration notification : notificationConfigurations) {

					if (notification.getTaskId() == null || !notification.getTaskId().equals(node.getId())
							|| notification.getTriggerPointId() == null) {
						continue;
					}

					Integer triggerPointId;

					try {
						triggerPointId = Integer.valueOf(notification.getTriggerPointId());
					} catch (NumberFormatException ex) {
						continue;
					}

					if (triggerPointId == 3 || triggerPointId == 4) {

						formSubmissionNotifications.add(notification);

					} else if (triggerPointId == 1) {

						taskCompletionNotifications.add(notification);
					}
				}
			}
			
			ActivityMapDTO.ActivityData fsActivity = new ActivityMapDTO.ActivityData();

			fsActivity.setActivityType("FS");
			fsActivity.setActivityName(getActivityName("FS"));

			activityDataList.add(fsActivity);

			for (NotificationConfiguration notification : formSubmissionNotifications) {

				addNotificationActivity(activityDataList, notification);
			}

			/*
			 * Document Generation
			 */
			List<DocMappingDTO> documentMappings = node.getData().getDocumentMapping();

			if (documentMappings != null && !documentMappings.isEmpty()) {

				ActivityMapDTO.ActivityData data = new ActivityMapDTO.ActivityData();

				data.setActivityType("DG");
				data.setActivityName(getActivityName("DG"));
				data.setUserSubmissionRequired(requiresDocumentSubmission(node, documentMappings));

				activityDataList.add(data);
			}
			for (NotificationConfiguration notification : taskCompletionNotifications) {
				addNotificationActivity(activityDataList, notification);
			}

			for (int i = 0; i < activityDataList.size(); i++) {

				ActivityMapDTO.ActivityData data = activityDataList.get(i);

				data.setIndex(i);
				data.setIsLast(i == activityDataList.size() - 1);
			}

			ActivityMapDTO dto = new ActivityMapDTO();
			dto.setTaskId(node.getId());
			dto.setData(activityDataList);

			activityFlowList.add(dto);
		}

		return activityFlowList;
	}
	private String getActivityName(String code) {

        return switch (code) {
            case "PDE" -> "Predefined Enclosure";
            case "FS"  -> "Form Submit";
            case "DG"  -> "Document Generation";
            case "ES"  -> "Enclosure Submit";
            case "NG"  -> "Notification Genration";
            default    -> "Unknown";
        };
    }
	
	private void addNotificationActivity(List<ActivityMapDTO.ActivityData> activityDataList,
			NotificationConfiguration notification) {

		ActivityMapDTO.ActivityData data = new ActivityMapDTO.ActivityData();

		data.setActivityType("NG");
		data.setActivityName(getActivityName("NG"));
		data.setActivityConfigId(notification.getNotificationId());
		data.setUserSubmissionRequired(false);

		activityDataList.add(data);
	}

    private boolean requiresDocumentSubmission(NodeDTO node,List<DocMappingDTO> mappings) {

        if (ApplicationConstants.SUBMISSION_NODE_TYPE.equals(node.getType())) {
            return false;
        }

        if (node.getData().getTaskType() == null || node.getData().getHumanTask() == null) {
            return false;
        }

        if ((Integer) node.getData().getTaskType().getValue() != 1) {
            return false;
        }

        return (Integer) node.getData().getHumanTask().getValue() == 1;
    }

    private List<EscalationDetailsDTO> buildEscalationDetails(List<NodeDTO> nodes) {

        List<EscalationDetailsDTO> escalationDetails = new ArrayList<>();

        for (NodeDTO node : nodes) {

            if (node.getData() == null) {
                continue;
            }

            TaskType taskType = TaskType.resolve(node.getData());

            if (taskType != TaskType.OFFICIAL_TASK
                    && taskType != TaskType.WEB_SERVICE_TASK
                    && taskType != TaskType.APPLICANT_TASK) {
                continue;
            }

            Escalation escalation = node.getData().getEscalation();
			if (escalation != null &&  escalation.getEnabled() != null && escalation.getEnabled()) {
				EscalationDetailsDTO dto = new EscalationDetailsDTO();
				dto.setTaskId(node.getId());
				dto.setTaskName(node.getData().getName());

				dto.setSlaPeriod(escalation.getSlaPeriod());
				dto.setEscalationPeriod(escalation.getEscalationPeriod());
				dto.setMvelExpression(escalation.getMvelExpression());

				if (escalation.getDefaultActions() != null) {
					dto.setDefaultActions(escalation.getDefaultActions().stream().map(LabelValue::getValue)
							.map(String::valueOf).toList());
				}

				escalationDetails.add(dto);
			}
        }

        return escalationDetails;
    }
    
    private List<TimerTaskDTO> buildTimerTaskDTO(List<NodeDTO> nodes) {

        List<TimerTaskDTO> timerTaskList = new ArrayList<>();

        
        if (nodes == null || nodes.isEmpty()) {
            return timerTaskList;
        }

        for (NodeDTO nodeDTO : nodes) {
        	
        	if (!ApplicationConstants.TASK.equalsIgnoreCase(nodeDTO.getType())) {
	            continue;
	        }
        	
            if (nodeDTO.getData() == null
                    || nodeDTO.getData().getAdvanced() == null
                    || nodeDTO.getData().getAdvanced().getTimer() == null) {
                continue;
            }
            TaskType taskType = TaskType.resolve(nodeDTO.getData());

            if (taskType != TaskType.TIMER_TASK) {
                continue;
            }
            Timer timer = nodeDTO.getData().getAdvanced().getTimer();

            if (timer.getTaskExecustionPeriod() == null
                    || timer.getTaskExecustionPeriod().getBehaviour() == null) {
                continue;
            }

            TimerTaskDTO dto = new TimerTaskDTO();

            dto.setTaskId(nodeDTO.getId());

            Integer behaviour = (Integer)timer.getTaskExecustionPeriod()
                                     .getBehaviour()
                                     .getValue();

            dto.setBehaviour(behaviour);

            // Static Behaviour
            if (Integer.valueOf(2).equals(behaviour)
                    && timer.getTaskExecustionPeriod().getStaticValue() != null) {

                Static staticValue = timer.getTaskExecustionPeriod().getStaticValue();

                dto.setExecutionPeriod(staticValue.getTaskExecutionPeriod());

                if (staticValue.getUnit() != null) {
                    dto.setExecutionPeriodUnit(staticValue.getUnit().getLabel());
                }
            }

            timerTaskList.add(dto);
        }

        return timerTaskList;
    }

	private List<NotificationDetails> buildNotificationDetails(
			List<NotificationConfiguration> notificationConfigurations) {
		List<NotificationDetails> notificationDetails = new ArrayList<>();
		NotificationConfigEvent event = new NotificationConfigEvent();
		Map<Long, NotificationTempleConfig> notificationTempleConfigMap = new HashMap<Long, NotificationConfigEvent.NotificationTempleConfig>();
		Map<Long, NotificationProviderConfig> notificationProviderMap = new HashMap<Long, NotificationConfigEvent.NotificationProviderConfig>();

		for (NotificationConfiguration notifConfig : notificationConfigurations) {
			NotificationDetails detail = new NotificationDetails();
			detail.setNotificationId(notifConfig.getNotificationId());
			detail.setTriggerPointId(notifConfig.getTriggerPointId());
			detail.setTaskId(notifConfig.getTaskId());
			notificationDetails.add(detail);

			NotificationTempleConfig notificationTempleConfig = new NotificationTempleConfig();
			notificationTempleConfig.setTaskId(notifConfig.getTaskId());
			notificationTempleConfig.setNotificationId(notifConfig.getNotificationId());
			notificationTempleConfig.setChannel(NotificationType.fromValue(notifConfig.getChannel()));

			NotificationProviderConfig notificationProviderConfig = new NotificationProviderConfig();
			notificationProviderConfig.setType(NotificationType.fromValue(notifConfig.getChannel()));

			Long notificationProviderId = 0L;
			switch (NotificationType.fromValue(notifConfig.getChannel())) {
			case EMAIL:
				notificationTempleConfig.setEmailConfig(notifConfig.getConfigJson().getEmailConfig());
				break;
			case SMS:
				notificationTempleConfig.setSmsConfig(notifConfig.getConfigJson().getSmsConfig());
				if (notifConfig.getConfigJson().getSmsConfig() != null) {
					SmsConfig smsConfig = notifConfig.getConfigJson().getSmsConfig();
					notificationProviderId = smsConfig.getSmsConfigId();
				}
				break;
			case SANDES:
				notificationTempleConfig.setSandesConfig(notifConfig.getConfigJson().getSandesConfig());
				if (notifConfig.getConfigJson().getSandesConfig() != null) {
					SandesConfig sandesConfig = notifConfig.getConfigJson().getSandesConfig();
					notificationProviderId = sandesConfig.getSandesConfigId();
				}
				break;
			case WHATSAPP:
				notificationTempleConfig.setWhatsappConfig(notifConfig.getConfigJson().getWhatsappConfig());
				if (notifConfig.getConfigJson().getWhatsappConfig() != null) {
					WhatsappConfig whatsappConfig = notifConfig.getConfigJson().getWhatsappConfig();
					notificationProviderId = whatsappConfig.getWhatsappConfigId();

				}
				break;
			}
			notificationTempleConfigMap.put(notificationTempleConfig.getNotificationId(), notificationTempleConfig);
			if (!Objects.equals(notificationProviderId, 0L) && !isNull(notificationProviderId)) {
				if (!notificationProviderMap.containsKey(notificationProviderId)) {
					com.serviceplus.metadata.notification.entity.NotificationProviderConfig providerConfig = notificationProviderConfigRepository
							.findById(notificationProviderId).orElse(null);
					if (providerConfig != null) {
						notificationProviderConfig.setId(providerConfig.getId());
						notificationProviderConfig.setType(NotificationType.fromValue(providerConfig.getType()));
						notificationProviderConfig.setConfigDTO(providerConfig.getConfigDTO());
						notificationProviderMap.put(notificationProviderId, notificationProviderConfig);
					}
				}
			}
		}

		if (!notificationConfigurations.isEmpty()) {
			event.setServiceId(notificationConfigurations.get(0).getServiceId());
		}

		event.setEventType("Freeze");
		event.setNotificationProviderConfigMap(notificationProviderMap);
		event.setNotificationTempleConfigMap(notificationTempleConfigMap);

		notificationKafkaProducer.publishFreezeEvent(event);
		return notificationDetails;
	}

	private void buildInboxSentboxFilters(List<InboxSentBoxFilter> inboxSentBoxFilters, Set<String> usedAttributeIds) {

		for (InboxSentBoxFilter inboxSentBoxFilter : inboxSentBoxFilters) {

			FilterJSONDTO filterJSON = inboxSentBoxFilter.getFilterJSON();
			if (filterJSON == null) {
				continue;
			}

			List<LabelValue> tasks = inboxSentBoxFilter.getTasks();

			if (tasks == null || tasks.isEmpty()) {
				continue;
			}
			addUsedAttributeIds(filterJSON.getFilterInput(), usedAttributeIds);
			addUsedAttributeIds(filterJSON.getFilterOutput(), usedAttributeIds);

			for (LabelValue task : tasks) {

				if (task == null || task.getValue() == null) {
					continue;
				}

				String taskId = task.getValue().toString();

			}
		}
	}
	
	private void addUsedAttributeIds(List<FilterAttributeDTO> filterAttributes, Set<String> usedAttributeIds) {

		if (filterAttributes == null) {
			return;
		}
		for (FilterAttributeDTO attribute : filterAttributes) {
			if (attribute != null && !Boolean.TRUE.equals(attribute.getIsSystemVar()) && attribute.getId() != null) {
				usedAttributeIds.add(attribute.getId());
			}
		}
	}
	
	private FilterKafkaMessage buildInboxSentBoxFilterConfigKafkaMessage(List<InboxSentBoxFilter> inboxSentBoxFilters) {

		FilterKafkaMessage kafkaMessage = new FilterKafkaMessage();

		List<FilterKafkaDTO> configurations = new ArrayList<>();

		for (InboxSentBoxFilter filter : inboxSentBoxFilters) {

			if (filter == null) {
				continue;
			}

			if (filter.getTasks() == null || filter.getTasks().isEmpty()) {
				continue;
			}

			for (LabelValue task : filter.getTasks()) {

				if (task == null || task.getValue() == null) {
					continue;
				}

				String taskId = String.valueOf(task.getValue());

				if (filter.getFilterJSON() != null && filter.getFilterJSON().getFilterInput() != null) {

					for (FilterAttributeDTO attribute : filter.getFilterJSON().getFilterInput()) {

						if (attribute == null || attribute.getId() == null) {
							continue;
						}

						FilterKafkaDTO dto = buildKafkaDTO(filter, taskId, attribute, 'I');

						configurations.add(dto);
					}
				}
				if (filter.getFilterJSON() != null && filter.getFilterJSON().getFilterOutput() != null) {

					for (FilterAttributeDTO attribute : filter.getFilterJSON().getFilterOutput()) {

						if (attribute == null || attribute.getId() == null) {
							continue;
						}

						FilterKafkaDTO dto = buildKafkaDTO(filter, taskId, attribute, 'O');

						configurations.add(dto);
					}
				}
			}
		}

		kafkaMessage.setConfigurations(configurations);

		return kafkaMessage;
	}

	private FilterKafkaDTO buildKafkaDTO(InboxSentBoxFilter filter, String taskId, FilterAttributeDTO attribute,
			Character attrType) {

		FilterKafkaDTO dto = new FilterKafkaDTO();

		dto.setFilterId(filter.getId());
		dto.setBaseServiceId(filter.getBaseServiceId());
		dto.setServiceId(filter.getServiceId());
		dto.setTaskId(taskId);
		dto.setFilterType(filter.getFilterType());
		dto.setTenantId(filter.getTenantId());

		dto.setAttrType(attrType);
		dto.setAttrId(attribute.getId());
		dto.setAttrLabel(attribute.getAttrLabel());
		dto.setAttrFormId(attribute.getFormId());
		dto.setAttrOrder(attribute.getSortOrder());

		dto.setFilterCondition(filter.getCondition());

		return dto;
	}
	
	private Set<String> getAttributeUsedInNotification(List<NotificationConfiguration> notificationConfigurations) {
		Set<String> notificationUsedAttrSet = new HashSet<>();
		for (NotificationConfiguration notifConfig : notificationConfigurations) {
			if (notifConfig == null || notifConfig.getConfigJson() == null) {
				continue;
			}
			Set<String> usedAttr = null;
			switch (NotificationType.fromValue(notifConfig.getChannel())) {
			case EMAIL:
				if (notifConfig.getConfigJson().getEmailConfig() != null) {
					usedAttr = notifConfig.getConfigJson().getEmailConfig().getUsedAttr();
				}
				break;
			case SMS:
				if (notifConfig.getConfigJson().getSmsConfig() != null) {
					usedAttr = notifConfig.getConfigJson().getSmsConfig().getUsedAttr();
				}
				break;
			case SANDES:
				if (notifConfig.getConfigJson().getSandesConfig() != null) {
					usedAttr = notifConfig.getConfigJson().getSandesConfig().getUsedAttr();
				}
				break;
			case WHATSAPP:
				if (notifConfig.getConfigJson().getWhatsappConfig() != null) {
					usedAttr = notifConfig.getConfigJson().getWhatsappConfig().getUsedAttr();
				}
				break;
			}
			if (usedAttr != null) {
				notificationUsedAttrSet.addAll(usedAttr);
			}
		}
		return notificationUsedAttrSet;
	}

	private void updateMetadataJson(ServiceMetadata metadata, ServiceDefinition serviceDefinition, NodeDTO submissionNode,
            ServiceProcessFlowDTO processFlowDTO, Map<String, String> taskFormMapping, List<ActivityMapDTO> activityFlowMap,
            List<OfficeDetailsDTO> officeDetails, Set<String> documentUsedAttrSet, List<WorkFlowDataDTO> workFlowDetails,
            List<MvelDetailsDTO> mvelDetailsDTO, Set<String> mvelUsedAttrSet, List<ExternalAttributeMappingDTO> externalAttributeMappingDTO,
            List<OutputFormatFreezeDTO> outputFormatDetail, List<WebServiceTaskDTO> webServiceTasks, List<EscalationDetailsDTO> escalationDetails, List<TimerTaskDTO> timerTaskDetails, List<DocumentGenerationDetails> documentGenerationDetails,
            List<ServiceDeliveryUnitMappingDTO> serviceDeliveryUnitMappingWithUser, List<NotificationDetails> notificationDetails, Set<String> notificationUsedAttrSet,
            Set<String> inboxSentboxFiltersUsedAttrList,List<AuaApiConfigurationDTO> auaApiConfigurations) {

        ServiceJSONDTO serviceJson = metadata.getMetadataJson();
        if (serviceJson == null) {
            serviceJson = new ServiceJSONDTO();
        }

        serviceJson.setServiceId(serviceDefinition.getServiceId());
        serviceJson.setServiceName(serviceDefinition.getDefinitionJson().getName());
        serviceJson.setStateId(serviceDefinition.getStateId());
        serviceJson.setDepartmentId(serviceDefinition.getDepartmentId());
        serviceJson.setServiceAbbrevation(serviceDefinition.getDefinitionJson().getAbbreviation());

        serviceJson.setSubmissionModes(submissionNode.getData().getMode());
        serviceJson.setPredefinedEnclosures(submissionNode.getData().getPredefined());
        serviceJson.setEnclosures(submissionNode.getData().getEnclosures());
        serviceJson.setSubmissionLimitData(submissionNode.getData().getLimitSubmission());
        serviceJson.setApplFormId(submissionNode.getData().getFormDetail().getFormId());
        serviceJson.setApplSubmissionTaskId(submissionNode.getId());

        serviceJson.setProcessFlowMap(processFlowDTO);
        serviceJson.setTaskFormMapping(taskFormMapping);
        serviceJson.setActivityMap(activityFlowMap);
        serviceJson.setDocumentGenerationDetails(documentGenerationDetails);
        serviceJson.setOfficeDetails(officeDetails);
        serviceJson.setDocumentUsedAttrList(documentUsedAttrSet);
        serviceJson.setMvelUsedAttrList(mvelUsedAttrSet);
        serviceJson.setWorkFlowDetails(workFlowDetails);
        serviceJson.setMvelDetailsDTO(mvelDetailsDTO);
        serviceJson.setExternalAttributeMapping(externalAttributeMappingDTO);
        serviceJson.setOutputFormatDetails(outputFormatDetail);
        serviceJson.setWebServiceTasks(webServiceTasks);
        serviceJson.setEscalationDetails(escalationDetails);
        serviceJson.setTimerTaskDetails(timerTaskDetails);
        serviceJson.setServiceDeliveryUnitMappingDetails(serviceDeliveryUnitMappingWithUser);
        serviceJson.setNotificationDetails(notificationDetails);
        serviceJson.setNotificationUsedAttrList(notificationUsedAttrSet);
        serviceJson.setInboxSentboxFiltersUsedAttrList(inboxSentboxFiltersUsedAttrList);
        serviceJson.setAuaApiConfigurations(auaApiConfigurations);
        metadata.setMetadataJson(serviceJson);
        saveWorkflowAssignments(metadata);
        saveServiceDeliveryUnits(metadata);

    }
    private List<WebServiceTaskDTO> buildWebServiceTasks(List<NodeDTO> nodes) {

	    List<WebServiceTaskDTO> taskList = new ArrayList<>();

	    for (NodeDTO node : nodes) {

	        if (!ApplicationConstants.TASK.equalsIgnoreCase(node.getType())) {
	            continue;
	        }
	        if (node.getData() == null) {
	            continue;
	        }
	        if (node.getData().getWebserviceDetails() == null) {
	            continue;
	        }
	        WebServiceTaskDTO dto = new WebServiceTaskDTO();
	        dto.setTaskId(node.getId());
	        dto.setTaskName(node.getData().getName());
	        dto.setWebserviceDetails(node.getData().getWebserviceDetails());

	        taskList.add(dto);
	    }

	    return taskList;
	}

    public void saveWorkflowAssignments(ServiceMetadata serviceMetadata) {

        List<OfficeDetailsDTO> officeDetails = serviceMetadata.getMetadataJson().getOfficeDetails();
        List<LabelValue> submissionModes = serviceMetadata.getMetadataJson().getSubmissionModes();
        
        Set<Integer> submissionModeSet = submissionModes == null
                ? Collections.emptySet()
                : submissionModes.stream()
                        .map(mode -> Integer.valueOf(mode.getValue().toString()))
                        .collect(Collectors.toSet());

        boolean skipApplicantTask = !submissionModeSet.isEmpty()
                && submissionModeSet.stream().allMatch(mode ->
                        ApplicationConstants.SUBMISSION_MODE_APPLICANT.equals(mode)
                                || ApplicationConstants.SUBMISSION_MODE_CLOSED_USER_GROUP.equals(mode));
        
        if (officeDetails == null || officeDetails.isEmpty()) {
            return;
        }

        Date now = new Date();

        List<WorkflowAssignment> assignments = new ArrayList<>();

        for (OfficeDetailsDTO officeDetail : officeDetails) {
        	if (skipApplicantTask && ApplicationConstants.SUBMISSION_NODE_ID.equals(officeDetail.getTaskId())) {
                continue;
            }
            List<OfficeDetailsDTO.OfficeUnitData> allowedOffices = mapper.convertValue(officeDetail.getAllowedOffices(),new TypeReference<List<OfficeDetailsDTO.OfficeUnitData>>() {});
            if(allowedOffices==null) {
                continue;
            }
            
            for (OfficeDetailsDTO.OfficeUnitData office : allowedOffices) {
                List<WorkflowAssignment> assignmentList = workFlowAssignmentRepository.findByServiceIdAndTaskIdAndLocationId(serviceMetadata.getServiceId(), officeDetail.getTaskId(),
                        office.getOrgUnitCode().toString());
                if (assignmentList.isEmpty()) {
                    WorkflowAssignment assignment = new WorkflowAssignment();
                    assignment.setHolderId(ApplicationConstants.WORKFLOW_ASSIGNMENT_TOKEN_PREFIX + createUniqueId());
                    assignment.setCrtOn(now);
                    assignment.setAssignedBy(0l);
                    assignment.setBaseServiceId(serviceMetadata.getBaseServiceId());
                    assignment.setServiceId(serviceMetadata.getServiceId());
                    assignment.setTaskId(officeDetail.getTaskId());
                    assignment.setLocationId(office.getOrgUnitCode() != null ? office.getOrgUnitCode().toString() : null);
                    assignment.setVersionNo(serviceMetadata.getVersionNo());
                    assignment.setTenantId(serviceMetadata.getTenantId());
                    assignments.add(assignment);
                }
            }
        }
        List<WorkflowAssignment> saveAll = workFlowAssignmentRepository.saveAll(assignments);
    }
    
	private void saveServiceDeliveryUnits(ServiceMetadata serviceMetadata) {
		List<ServiceDeliveryUnitMappingDTO> mappings = serviceMetadata.getMetadataJson()
				.getServiceDeliveryUnitMappingDetails();
		if (mappings == null || mappings.isEmpty()) {
			return;
		}
		Date now = new Date();
		List<ServiceDeliveryUnits> unitsToSave = new ArrayList<>();
		for (ServiceDeliveryUnitMappingDTO mapping : mappings) {
			if (mapping.getOfficeUnits() == null || mapping.getOfficeUnits().isEmpty()) {
				continue;
			}
			for (OfficeDetailsDTO.OfficeUnitData office : mapping.getOfficeUnits()) {
				Optional<ServiceDeliveryUnits> existing = serviceDeliveryUnitsRepository
						.findByTenantIdAndServiceIdAndDeliveryUnitLevelIdAndLocationIdAndDeletedFlagFalse(
								serviceMetadata.getTenantId(), serviceMetadata.getServiceId(),
								mapping.getDeliveryUnitLevelId(), office.getOrgUnitCode());
				if (existing.isPresent()) {
					continue;
				}
				ServiceDeliveryUnits entity = new ServiceDeliveryUnits();
				entity.setTenantId(serviceMetadata.getTenantId());
				entity.setServiceId(serviceMetadata.getServiceId());
				entity.setBaseServiceId(serviceMetadata.getBaseServiceId());
				entity.setDeliveryUnitLevelId(mapping.getDeliveryUnitLevelId());
				entity.setLocationId(office.getOrgUnitCode());
				entity.setLocationName(office.getOrgUnitName());
				entity.setActivationStatus(0);
				entity.setCreatedBy(0L);
				entity.setCreatedOn(now);
				entity.setDeletedFlag(false);
				unitsToSave.add(entity);
			}
		}
		if (!unitsToSave.isEmpty()) {
			serviceDeliveryUnitsRepository.saveAll(unitsToSave);
		}
	}
    
    public ResponseEntity<?> serviceParameterList(Integer serviceId, Long moduleId, UserSessionDTO userSessionDetails) {

        JSONObject responseJson = new JSONObject();

        ServiceParameterDetailsDTO responseDTO = new ServiceParameterDetailsDTO();

        List<ServiceParameterDetailsDTO.TemplateDetails> templateList = new ArrayList<>();
        List<ServiceParameterDetailsDTO.TaskDetails> taskList = new ArrayList<>();
        List<ServiceParameterDetailsDTO.EnclosureDetails> enclosureDetails = new ArrayList<>();
        List<ServiceParameterDetailsDTO.SystemAttribute> systemAttributeDetails = new ArrayList<>();

        try {
            responseDTO.setServiceId(serviceId);
            ServiceProcessFlow serviceProcessFlow =processFlowRepository.findByServiceIdAndUserId(serviceId, userSessionDetails.getUserID().longValue());
            if (serviceProcessFlow != null && serviceProcessFlow.getProcessFlowJson() != null) {
                List<NodeDTO> nodes =serviceProcessFlow.getProcessFlowJson().getNodes();
                for (NodeDTO node : nodes) {
                    TaskDetails task = new TaskDetails();
                    List<Enclosure> enclosureList=node.getData()!=null && node.getData().getEnclosures()!=null? new ArrayList(node.getData().getEnclosures()):new ArrayList<>();
                    task.setTaskId(node.getId());
                    task.setTaskName(node.getData() != null ? node.getData().getName() : "");
                    task.setType(ApplicationConstants.TASK);
                    taskList.add(task);
                    if(!enclosureList.isEmpty()) {
                        for(Enclosure enclosure:enclosureList) {
                            ServiceParameterDetailsDTO.EnclosureDetails enclosureDetail = new ServiceParameterDetailsDTO.EnclosureDetails();
                            enclosureDetail.setEnclosureId(node.getId()+"_"+enclosure.getEnclosureType().getValue());
                            enclosureDetail.setEnclosureLabel(enclosure.getEnclosureType().getLabel());
                            enclosureDetail.setType(ApplicationConstants.ENCLOSURE);
                            enclosureDetails.add(enclosureDetail);
                        }
                    }

                }
            }
            responseDTO.setTaskDetails(taskList);
            responseDTO.setEnclosureDetails(enclosureDetails);
            List<SystemVariableMaster> systemVariableList=systemVariableRepository.findByModuleIdOrModuleIdIsNull(moduleId);
            for(SystemVariableMaster systemVariable:systemVariableList) {
                ServiceParameterDetailsDTO.SystemAttribute sysAttrDetail = new ServiceParameterDetailsDTO.SystemAttribute();
                sysAttrDetail.setAttributeId(systemVariable.getAttributeId());
                sysAttrDetail.setAttributeLabel(systemVariable.getAttributeLabel());
                sysAttrDetail.setType(ApplicationConstants.SYSTEM_ATTRIBUTE);
                systemAttributeDetails.add(sysAttrDetail);
            }
            responseDTO.setSystemAttributes(systemAttributeDetails);
            List<ServiceTemplate> serviceTemplateList =serviceTemplateRepository.findByServiceIdAndUserId(serviceId, userSessionDetails.getUserID().longValue());
            for (ServiceTemplate serviceTemplate : serviceTemplateList) {

                ServiceParameterDetailsDTO.TemplateDetails templateDTO =new ServiceParameterDetailsDTO.TemplateDetails();

                templateDTO.setTemplateName(serviceTemplate.getTemplateName());
                templateDTO.setTaskId(serviceTemplate.getTaskId());
                templateDTO.setFormId(serviceTemplate.getFormId());
                templateDTO.setHolderId(serviceTemplate.getHolderId());
                ResponseEntity<?> formDtoResponse =formFeignClient.getAllAttributesByFormId(serviceTemplate.getFormId());

                if (formDtoResponse.getStatusCode().equals(HttpStatus.OK)) {
                    Map<Object, Object> responseObj = (Map<Object, Object>) formDtoResponse.getBody();
                    JSONObject json = new JSONObject(responseObj);
                    FormDTO formDto = new Gson().fromJson(json.toString(), FormDTO.class);
                    List<Attribute> attrList = formDto.getAttributes();
                    List<ServiceParameterDetailsDTO.Attribute> attributeDTOList = new ArrayList<>();
                    for (Attribute attr : attrList) {
                        ServiceParameterDetailsDTO.Attribute attributeDTO =new ServiceParameterDetailsDTO.Attribute();

                        String attrId = createAttributeId(serviceTemplate.getTaskId(),serviceTemplate.getHolderId(),attr.getAttributeId());
                        attributeDTO.setAttributeId(attrId);
                        attributeDTO.setAttributeLabel(attr.getAttributeLabel());
                        attributeDTO.setTypeId(attr.getTypeId());
                        attributeDTO.setType(ApplicationConstants.ATTRIBUTE);
                        attributeDTOList.add(attributeDTO);
                    }
                    templateDTO.setAttributeList(attributeDTOList);
                }
                templateList.add(templateDTO);
            }
            responseDTO.setTemplateDetails(templateList);
        } catch (Exception ex) {
            ex.printStackTrace();

            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");

            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok(responseDTO);
    }
    public ResponseEntity<?> unfreeze(Integer serviceId, UserSessionDTO userSessionDetails) {
        try {
            ServiceDefinition serviceDefinition = serviceDefinitionRepository.findById(serviceId).orElseThrow(() -> new SPRuntimeError("Service not found", HttpStatus.BAD_REQUEST));
            ServiceLog serviceLog = serviceLogRepository.findByServiceId(serviceId);
            ServiceMetadata serviceMetadata = metadataRepository.findByServiceIdAndTenantId(serviceId,userSessionDetails.getTenantId());

            if(serviceLog== null) {
                new SPRuntimeError("Service not found", HttpStatus.BAD_REQUEST);
            }

            if (!serviceLog.getTenantId().equals(userSessionDetails.getTenantId())
                    || !serviceDefinition.getDepartmentId().equals(userSessionDetails.getEntityId().longValue())
                    || !serviceDefinition.getUserId().equals(userSessionDetails.getUserID().longValue())) {
                new SPRuntimeError("Unauthorized access", HttpStatus.BAD_REQUEST);
            }

            if(serviceLog.getServiceStatus().equals(ServiceStatus.LAUNCHED.getCode())){
                new SPRuntimeError("Service is in online mode. You can't unfreeze it.", HttpStatus.BAD_REQUEST);
            }

            Date date = new Date();
            serviceDefinition.setuDate(date);

            serviceLog.setServiceStatus(ServiceStatus.DEFINED.getCode());
            serviceLog.setuDate(date);
            if(!Objects.isNull(serviceMetadata)) {
                metadataRepository.delete(serviceMetadata);
            }
            String redisKey = ApplicationConstants.REDIS_KEY_OF_SERVICEMETADATA + serviceId;
            evictRedisCache(redisKey);
            serviceDefinitionRepository.save(serviceDefinition);
            serviceLogRepository.save(serviceLog);
            MvelFunctionEvent mvelFnEvent=new MvelFunctionEvent();
            mvelFnEvent.setEventType("Unfreeze");
            mvelFnEvent.setServiceId(serviceId);
            mvelKafkaProducer.publishUnfreezeEvent(mvelFnEvent);
            
            NotificationConfigEvent notifEvent = new NotificationConfigEvent();
            notifEvent.setServiceId(serviceId);
            notifEvent.setEventType("Unfreeze");
            notificationKafkaProducer.publishFreezeEvent(notifEvent);
            
            return ResponseEntity.ok(Map.of("message", "Service unfrozen successfully"));
        } catch (Exception ex) {
            if (ex instanceof SPRuntimeError) {
                throw ex;
            } else {
                ex.printStackTrace();
                throw new SPRuntimeError("Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }
    public void evictRedisCache(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }


    public ServiceFormDefinitionDTO getServiceFormDefinition(Integer baseServiceId) {
        try {
            ServiceFormDefinitionDTO serviceFormDefinition = new ServiceFormDefinitionDTO();
            ServiceMetadata serviceMetadata = metadataRepository
                    .findTopByBaseServiceIdOrderByServiceIdDesc(baseServiceId).orElse(null);
            if (serviceMetadata == null) {
                return null;
            }
            serviceFormDefinition.setServiceId(serviceMetadata.getServiceId());
            serviceFormDefinition.setTaskId(serviceMetadata.getMetadataJson().getApplSubmissionTaskId());
            serviceFormDefinition.setFormId(serviceMetadata.getMetadataJson().getApplFormId());
            serviceFormDefinition.setFormBlankJsonDTO(new FormBlankJsonDTO());// blank json?
            return serviceFormDefinition;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }

    public ResponseEntity<?> getUserAssignmentServiceList(UserSessionDTO userSessionDetails) {
        JSONObject responseJson = new JSONObject();
        try {
        	List<ServiceAssignmentDTO> eligibleServices = new ArrayList<>();
			List<Integer> serviceIds = serviceLogRepository.findServiceIdByServiceStatusNotInAndTenantId(
					List.of(ServiceStatus.LAUNCHED.getCode(), ServiceStatus.DEFINED.getCode()),
					userSessionDetails.getTenantId());

			if (serviceIds == null || serviceIds.isEmpty()) {
				return new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);
			}

			List<ServiceMetadata> serviceList = metadataRepository.findByServiceIdIn(serviceIds);

			if (serviceList == null || serviceList.isEmpty()) {
				return new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);
			}
            Integer userOfficeLevelId = userSessionDetails.getEntityLevelId();
            eligibleServices = serviceList.stream()
                    .filter(service -> isServiceMappedToOfficeLevel(service, userOfficeLevelId))
                    .map(service -> new ServiceAssignmentDTO(
                            service.getServiceId(),
                            service.getMetadataJson().getServiceName()))
                    .collect(Collectors.toList());
            return new ResponseEntity<>(eligibleServices, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isServiceMappedToOfficeLevel(ServiceMetadata service, Integer officeLevelId) {
        if (service == null || service.getMetadataJson() == null|| service.getMetadataJson().getOfficeDetails() == null) {
            return false;
        }
        for (OfficeDetailsDTO officeDetail : service.getMetadataJson().getOfficeDetails()) {
            if (officeDetail.getOfficeLevelIds() != null && officeDetail.getOfficeLevelIds().contains(officeLevelId)) {
                return true;
            }
        }
        return false;
    }

    public ResponseEntity<?> getUserAssignmentTaskList(Integer serviceId, Integer officeLevelId,UserSessionDTO userSessionDetails) {
        JSONObject responseJson = new JSONObject();
        try {
        	Optional<ServiceLog> serviceLog = serviceLogRepository.findByServiceIdAndServiceStatusNotInAndTenantId(serviceId,
					List.of(ServiceStatus.LAUNCHED.getCode(), ServiceStatus.DEFINED.getCode()),
					userSessionDetails.getTenantId());

			if (serviceLog.isEmpty()) {
				return new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);
			}
            ServiceMetadata serviceMetadata = metadataRepository.findByServiceIdAndTenantId(serviceId,userSessionDetails.getTenantId());
            if (serviceMetadata == null || serviceMetadata.getMetadataJson() == null) {
                return new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);
            }

            List<OfficeDetailsDTO> officeDetails = serviceMetadata.getMetadataJson().getOfficeDetails();
            if (officeDetails == null || officeDetails.isEmpty()) {
                return new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);
            }
            List<LabelValue> submissionModes = serviceMetadata.getMetadataJson().getSubmissionModes();

            Set<Integer> submissionModeSet = submissionModes == null
                    ? Collections.emptySet()
                    : submissionModes.stream()
                            .map(mode -> Integer.valueOf(mode.getValue().toString()))
                            .collect(Collectors.toSet());

            boolean skipSubmissionTask = !submissionModeSet.isEmpty()
                    && submissionModeSet.stream().allMatch(mode ->
                            ApplicationConstants.SUBMISSION_MODE_APPLICANT.equals(mode)
                                    || ApplicationConstants.SUBMISSION_MODE_CLOSED_USER_GROUP.equals(mode));
            
            List<TaskAssignmentDTO> taskList = officeDetails.stream()
                    .filter(officeDetail -> officeDetail.getOfficeLevelIds() != null
                            && officeDetail.getOfficeLevelIds().contains(officeLevelId))
                    .filter(officeDetail -> !(skipSubmissionTask
                                    && ApplicationConstants.SUBMISSION_NODE_ID.equals(officeDetail.getTaskId())))
                    .map(officeDetail -> new TaskAssignmentDTO(officeDetail.getTaskId(),
                            getTaskName(serviceMetadata, officeDetail.getTaskId())))
                    .collect(
                            Collectors.collectingAndThen(
                                    Collectors.toMap(TaskAssignmentDTO::getTaskId, Function.identity(),
                                            (existing, replacement) -> existing),
                                    map -> new ArrayList<>(map.values())));

            return new ResponseEntity<>(taskList, HttpStatus.OK);

        } catch (Exception ex) {

            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");

            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getTaskName(ServiceMetadata serviceMetadata, String taskId) {

        ServiceProcessFlowDTO processFlow = serviceMetadata.getMetadataJson().getProcessFlowMap();

        if (processFlow == null || processFlow.getData() == null) {
            return taskId;
        }

        return processFlow.getData().stream()
                .filter(data -> data.getNode() != null && taskId.equals(data.getNode().getId()))
                .map(data -> data.getNode().getName()).findFirst().orElse(taskId);
    }

    public ResponseEntity<?> getOutputFormatDetails(Integer serviceId, Long outputFormatId) {

        JSONObject responseJson = new JSONObject();

        try {

            Optional<ServiceMetadata> serviceMetadataOpt = metadataRepository.findById(serviceId);

            if (serviceMetadataOpt.isEmpty()) {

                responseJson.put("errorCode", "404");
                responseJson.put("errorMessage", "Service metadata not found");

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseJson.toString());
            }

            ServiceMetadata serviceMetadata = serviceMetadataOpt.get();

            List<OutputFormatFreezeDTO> outputFormatDetails = serviceMetadata.getMetadataJson().getOutputFormatDetails();

            if (outputFormatDetails == null || outputFormatDetails.isEmpty()) {

                responseJson.put("errorCode", "404");
                responseJson.put("errorMessage", "No output format details found");

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseJson.toString());
            }

            OutputFormatFreezeDTO outputFormat = outputFormatDetails.stream().filter(item -> outputFormatId.equals(item.getId())).findFirst().orElse(null);

            if (outputFormat == null) {

                responseJson.put("errorCode", "404");
                responseJson.put("errorMessage", "Output format not found");

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseJson.toString());
            }

            return ResponseEntity.ok(outputFormat);

        } catch (Exception ex) {
            ex.printStackTrace();
            responseJson.put("errorCode", "500");
            responseJson.put("errorMessage", "Internal Server Error");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseJson.toString());
        }
    }

	public ResponseEntity<?> getServiceDeliveryUnitList(Integer serviceId, Integer officeLevelId,
			UserSessionDTO userSessionDetails) {
		JSONObject responseJson = new JSONObject();
		try {
			Optional<ServiceLog> serviceLog = serviceLogRepository.findByServiceIdAndServiceStatusNotInAndTenantId(serviceId,
					List.of(ServiceStatus.LAUNCHED.getCode(), ServiceStatus.DEFINED.getCode()),
					userSessionDetails.getTenantId());

			if (serviceLog.isEmpty()) {
				return new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);
			}
			ServiceMetadata serviceMetadata = metadataRepository.findByServiceIdAndTenantId(serviceId,
					userSessionDetails.getTenantId());
			if (serviceMetadata == null || serviceMetadata.getMetadataJson() == null) {
				return ResponseEntity.ok(Collections.emptyList());
			}
			//Entity check required ??
			
			List<ServiceDeliveryUnitMappingDTO> mappings = serviceMetadata.getMetadataJson()
					.getServiceDeliveryUnitMappingDetails();
			if (mappings == null || mappings.isEmpty()) {
				return ResponseEntity.ok(Collections.emptyList());
			}
			List<ServiceDeliveryUnitResponseDTO> response = mappings.stream()
					.filter(mapping -> officeLevelId.equals(mapping.getDesignatedOfficerUserLevelId())).map(mapping -> {
						ServiceDeliveryUnitResponseDTO dto = new ServiceDeliveryUnitResponseDTO();
						dto.setRoleId(mapping.getUserRoleId());
						dto.setUserRole(mapping.getUserRole());
						dto.setDeliveryUnitLevelId(mapping.getDeliveryUnitLevelId());
						dto.setDeliveryUnitLevelName(mapping.getDeliveryUnitLevelName());
						dto.setDesignatedOfficerUserLevelId(mapping.getDesignatedOfficerUserLevelId());
						dto.setDesignatedOfficerUserLevelName(mapping.getDesignatedOfficerUserLevelName());
						serviceDesignatedOfficerAssignmentRepository
								.findFirstByTenantIdAndServiceIdAndDeliveryUnitLevelId(userSessionDetails.getTenantId(),
										serviceId, mapping.getDeliveryUnitLevelId())
								.ifPresent(entity -> {
									dto.setAssignedUserId(entity.getUserId());
									dto.setAssignedUserName(userService.getUserName(entity.getUserId()));
									dto.setAssignedDesignationId(entity.getDesignationId());
									DesignationRequestDTO requestDto=new DesignationRequestDTO();
									requestDto.setDesignationCode(entity.getDesignationId());
									dto.setDesignationName(userService.getDesignationName(requestDto));
								});
						return dto;
					}).collect(Collectors.toList());
			return ResponseEntity.ok(response);
		} catch (Exception ex) {
			responseJson.put("errorCode", "500");
			responseJson.put("errorMessage", "Internal Server Error");
			return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Transactional
	public ResponseEntity<?> activateService(ServiceActivationDTO request, UserSessionDTO userSessionDetail) {
		JSONObject response = new JSONObject();
		try {
			Optional<ServiceLog> serviceLog = serviceLogRepository.findByServiceIdAndServiceStatusNotInAndTenantId(request.getServiceId(),
					List.of(ServiceStatus.LAUNCHED.getCode(), ServiceStatus.DEFINED.getCode()),
					userSessionDetail.getTenantId());

			if (serviceLog.isEmpty()) {
				return new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);
			}
			Date now = new Date();
			for (Integer locationId : request.getLocationIds()) {
				Optional<ServiceDeliveryUnits> optional = serviceDeliveryUnitsRepository
						.findByTenantIdAndServiceIdAndDeliveryUnitLevelIdAndLocationIdAndDeletedFlagFalse(
								userSessionDetail.getTenantId(), request.getServiceId(),
								request.getDeliveryUnitLevelId(), locationId);
				ServiceDeliveryUnits entity;
				if (optional.isPresent()) {
					entity = optional.get();
					entity.setActivationStatus(request.getActivationStatus());
					entity.setModifiedBy(userSessionDetail.getUserID().longValue());
					entity.setModifiedOn(now);
					if (request.getActivationStatus() == 1) {
						entity.setActivatedBy(userSessionDetail.getUserID().longValue());
						entity.setActivatedOn(now);
					} else {
						entity.setDeactivatedBy(userSessionDetail.getUserID().longValue());
						entity.setDeactivatedOn(now);
					}
				} else {
					entity = new ServiceDeliveryUnits();
					entity.setTenantId(userSessionDetail.getTenantId());
					entity.setServiceId(request.getServiceId());
					entity.setBaseServiceId(request.getServiceId()/10000);
					entity.setDeliveryUnitLevelId(request.getDeliveryUnitLevelId());
					entity.setLocationId(locationId);
					entity.setActivationStatus(request.getActivationStatus());
					entity.setCreatedBy(userSessionDetail.getUserID().longValue());
					entity.setCreatedOn(now);
					if (request.getActivationStatus() == 1) {
						entity.setActivatedBy(userSessionDetail.getUserID().longValue());
						entity.setActivatedOn(now);
					} else {
						entity.setDeactivatedBy(userSessionDetail.getUserID().longValue());
						entity.setDeactivatedOn(now);
					}
					entity.setDeletedFlag(false);
				}
				serviceDeliveryUnitsRepository.save(entity);
			}
			response.put("message", "Service activation updated successfully.");
			return ResponseEntity.ok(response.toString());
		} catch (Exception ex) {
			response.put("errorMessage", "Internal Server Error");
			return new ResponseEntity<>(response.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Transactional
	public ResponseEntity<?> moveToProduction(Integer serviceId, UserSessionDTO userSessionDetails) {

	    try {
	    	Date currentDate = new Date();
	    	int updated = serviceLogRepository.moveToProduction(serviceId,userSessionDetails.getUserID().longValue(),currentDate,userSessionDetails.getTenantId());

	    	if (updated == 0) {
	    	    return ResponseEntity.badRequest()
	    	            .body("Service not found or not for ready to launch.");
	    	}
	        return ResponseEntity.ok("Service moved to production successfully.");

	    } catch (Exception ex) {
	    	ex.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("Internal Server Error");
	    }
	}
	
	public ServiceMetadata rebuildServiceMetaData(ServiceMetadata metadata, ServiceProcessFlow processFlow) {
		ServiceJSONDTO serviceJson = metadata.getMetadataJson();

		List<NodeDTO> nodes = processFlow.getProcessFlowJson().getNodes();
		List<EdgeDTO> edges = processFlow.getProcessFlowJson().getEdges();
		NodeDTO submissionNode = getSubmissionNode(nodes);
		Map<Object, List<OfficeDetailsDTO.OfficeUnitData>> entityLevelOfficeUnitMap = new HashMap<Object, List<OfficeDetailsDTO.OfficeUnitData>>();
		List<OfficeDetailsDTO> officeDetails = buildOfficeData(nodes, entityLevelOfficeUnitMap);
		Map<String, String> taskFormMapping = buildTaskFormMapping(nodes);

		ServiceProcessFlowDTO processFlowDTO = buildProcessFlowMap(metadata.getServiceId(), nodes, edges, officeDetails, taskFormMapping);
		List<ServiceProcessFlowDTO.AssociatedActivity> associatedActivities = buildAssociatedActivities(nodes, processFlow.getProcessFlowJson().getAssociatedTasks(), officeDetails, entityLevelOfficeUnitMap);

		processFlowDTO.setAssociatedActivities(associatedActivities);
		serviceJson.setTaskFormMapping(taskFormMapping);
		serviceJson.setApplFormId(submissionNode.getData().getFormDetail().getFormId());
		serviceJson.setProcessFlowMap(processFlowDTO);

		return metadata;
	}

	public void rebuildServiceMetaDataForInboxSentBoxFilter(Integer serviceId, FilterJSONDTO filterJSONDTO) {

		metadataRepository.findById(serviceId).ifPresent(serviceMetadata -> {
			if(serviceMetadata.getMetadataJson().getInboxSentboxFiltersUsedAttrList() != null && !serviceMetadata.getMetadataJson().getInboxSentboxFiltersUsedAttrList().isEmpty()){
				Set<String> usedAttributeIds = serviceMetadata.getMetadataJson().getInboxSentboxFiltersUsedAttrList();
				List<FilterAttributeDTO> filterInputList = filterJSONDTO.getFilterInput();
				for(FilterAttributeDTO filterAttribute : filterInputList) {
					usedAttributeIds.remove(filterAttribute.getId());
				}
				List<FilterAttributeDTO> filterOutputList = filterJSONDTO.getFilterOutput();
				for(FilterAttributeDTO filterAttribute : filterOutputList) {
					usedAttributeIds.remove(filterAttribute.getId());
				}
				serviceMetadata.getMetadataJson().setInboxSentboxFiltersUsedAttrList(usedAttributeIds);
				metadataRepository.save(serviceMetadata);
				evictRedisCache(ApplicationConstants.REDIS_KEY_OF_SERVICEMETADATA + serviceMetadata.getServiceId());
			}

		});
	}
	public Boolean isExternalServiceMapped(Integer serviceId, String clientId) {

		ServiceMetadata serviceMetadata = metadataRepository.findById(serviceId).orElse(null);

		if (serviceMetadata == null || serviceMetadata.getMetadataJson() == null) {
			return false;
		}

		List<ExternalAttributeMappingDTO> mappings = serviceMetadata.getMetadataJson().getExternalAttributeMapping();

		if (mappings == null || mappings.isEmpty()) {
			return false;
		}

		return mappings.stream().anyMatch(mapping -> clientId.equals(mapping.getExternalSystemRegId().toString()));
	}

	public List<ExternalClientFormAttributesMappingDTO> getExternalFormAttributes(Integer serviceId) {

		ServiceMetadata metadata = metadataRepository.findById(serviceId).orElse(null);

		if (metadata == null || metadata.getMetadataJson() == null
				|| metadata.getMetadataJson().getExternalAttributeMapping() == null) {
			return Collections.emptyList();
		}

		return metadata.getMetadataJson().getExternalAttributeMapping().stream()
				.filter(mapping -> mapping.getAttrMappingDetails() != null)
				.flatMap(mapping -> mapping.getAttrMappingDetails().stream()
						.map(attr -> toExternalFormAttributesDTO(mapping, attr, serviceId)))
				.collect(Collectors.toList());
	}

	private ExternalClientFormAttributesMappingDTO toExternalFormAttributesDTO(ExternalAttributeMappingDTO mapping,
			AttrMappingDetails attr, Integer serviceId) {

		ExternalClientFormAttributesMappingDTO dto = new ExternalClientFormAttributesMappingDTO();

		dto.setClientMappingId(mapping.getExternalSystemRegId());
		dto.setNodeName(mapping.getNodeId());
		dto.setNodeReference(attr.getNodeRef());
		dto.setAttributeId(attr.getAttributeId());
		dto.setServiceId(serviceId);

		return dto;
	}
}
