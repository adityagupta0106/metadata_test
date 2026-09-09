package com.serviceplus.metadata.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.serviceplus.metadata.dto.ServiceDeliveryUnitDTO;
import com.serviceplus.metadata.dto.UserSessionDTO;
import com.serviceplus.metadata.exception.SPRuntimeError;
import com.serviceplus.metadata.service.ServiceDeliveryUnitService;
import com.serviceplus.metadata.utility.CommonUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RequestMapping("/sdu")
@RestController
public class ServiceDeliveryUnitController {
	@Autowired
	private ServiceDeliveryUnitService deliveryUnitService;

	private static final Logger LOG = LogManager.getLogger("ServiceDeliveryUnitController");
	
	@GetMapping("/submission-level")
	public ResponseEntity<?> getSubmissionLocation(@RequestParam Integer serviceId, HttpServletRequest request) {
		JSONObject responseJson = new JSONObject();
		try {
			UserSessionDTO userSessionDetails = CommonUtil.getUserSessionDetails(request);
			ResponseEntity<?> tokenValidationResult = CommonUtil.validateServiceToken(request, serviceId, responseJson);
			if (tokenValidationResult != null) {
				return tokenValidationResult;
			}
			if (userSessionDetails == null) {
				throw new SPRuntimeError("Bad Request", HttpStatus.BAD_REQUEST);
			}
			return deliveryUnitService.getSubmissionLocation(serviceId, userSessionDetails);

		} catch (Exception ex) {
			if (ex instanceof SPRuntimeError) {
				throw ex;
			} else {
				LOG.error("Get Submission Location for service id {}, {}", serviceId, ex);
				ex.printStackTrace();
				throw new SPRuntimeError("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
	}

	@GetMapping("/get")
	public ResponseEntity<?> getServiceCoverage(@RequestParam Integer serviceId, HttpServletRequest request) {
		JSONObject responseJson = new JSONObject();
		try {
			UserSessionDTO userSessionDetails = CommonUtil.getUserSessionDetails(request);
			ResponseEntity<?> tokenValidationResult = CommonUtil.validateServiceToken(request, serviceId, responseJson);
			if (tokenValidationResult != null) {
				return tokenValidationResult;
			}
			if (userSessionDetails == null) {
				throw new SPRuntimeError("Bad Request", HttpStatus.BAD_REQUEST);
			}
			return deliveryUnitService.getServiceDeliveryUnit(serviceId, userSessionDetails);

		} catch (Exception ex) {
			if (ex instanceof SPRuntimeError) {
				throw ex;
			} else {
				LOG.error("Get Service Coverage for service id {}, {}", serviceId, ex);
				ex.printStackTrace();
				throw new SPRuntimeError("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
	}

	@PostMapping("/upsert")
	public ResponseEntity<?> saveServiceDeliveryUnit(@Valid @RequestBody ServiceDeliveryUnitDTO serviceDeliveryUnitDTO,
			HttpServletRequest request) {
		JSONObject responseJson = new JSONObject();
		try {
			UserSessionDTO userSessionDetails = CommonUtil.getUserSessionDetails(request);
			ResponseEntity<?> tokenValidationResult = CommonUtil.validateServiceToken(request,
					serviceDeliveryUnitDTO.getServiceId(), responseJson);
			if (tokenValidationResult != null) {
				return tokenValidationResult;
			}
			if (userSessionDetails == null) {
				throw new SPRuntimeError("Bad Request", HttpStatus.BAD_REQUEST);
			}
			return deliveryUnitService.saveServiceDeliveryUnit(serviceDeliveryUnitDTO, userSessionDetails);

		} catch (Exception ex) {
			if (ex instanceof SPRuntimeError) {
				throw ex;
			} else {
				LOG.error("Save Service Coverage for service id {}, {}", serviceDeliveryUnitDTO.getServiceId(), ex);
				ex.printStackTrace();
				throw new SPRuntimeError("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
	}

}
