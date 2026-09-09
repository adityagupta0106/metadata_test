package com.serviceplus.metadata.controller;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.serviceplus.metadata.dto.UserSessionDTO;
import com.serviceplus.metadata.service.ApplyService;
import com.serviceplus.metadata.utility.CommonUtil;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/apply")
public class ServiceApplyController {

	private final ApplyService applyService;

	public ServiceApplyController(ApplyService applyService) {
		this.applyService = applyService;
	}
	

	@PostMapping("/serviceMetaData")
	public ResponseEntity<?> serviceMetaData(@RequestParam Integer serviceId, HttpServletRequest request) {
		JSONObject responseJson = new JSONObject();
		try {
			UserSessionDTO userSessionDetails = CommonUtil.getUserSessionDetails(request);
			if (userSessionDetails == null) {
				responseJson.put("errorCode", "400");
				responseJson.put("errorMessage", "Bad Request");
				return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
			}
			return applyService.serviceMetaData(serviceId, userSessionDetails);
		} catch (Exception ex) {
			ex.printStackTrace();
			responseJson.put("errorCode", "500");
			responseJson.put("errorMessage", "Internal Server Error");
			return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@PostMapping("/resolveForm")
	public ResponseEntity<?> resolveForm(@RequestParam Integer baseServiceId,
			@RequestParam(required = false) Integer serviceId, @RequestParam(required = false) String taskId,
			HttpServletRequest request) {
		JSONObject responseJson = new JSONObject();
		try {
			UserSessionDTO userSessionDetails = CommonUtil.getUserSessionDetails(request);
			if (userSessionDetails == null) {
				responseJson.put("errorCode", "400");
				responseJson.put("errorMessage", "Bad Request");
				return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
			}
			return applyService.resolveForm(baseServiceId, serviceId, taskId, userSessionDetails);
		} catch (Exception ex) {
			responseJson.put("errorCode", "500");
			responseJson.put("errorMessage", "Internal Server Error");
			return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/processFlow")
	public ResponseEntity<?> processFlow(@RequestParam Integer serviceId, @RequestParam(required = false) String taskId,
			HttpServletRequest request) {
		JSONObject responseJson = new JSONObject();
		try {
			UserSessionDTO userSessionDetails = CommonUtil.getUserSessionDetails(request);
			if (userSessionDetails == null) {
				responseJson.put("errorCode", "400");
				responseJson.put("errorMessage", "Bad Request");
				return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
			}
			return applyService.processFlow(serviceId, taskId, userSessionDetails);
		} catch (Exception ex) {
			responseJson.put("errorCode", "500");
			responseJson.put("errorMessage", "Internal Server Error");
			return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/allowedAction")
	public ResponseEntity<?> applyAllowedAction(@RequestParam Integer serviceId, @RequestParam String taskId,
			HttpServletRequest request) {
		JSONObject responseJson = new JSONObject();
		try {
			UserSessionDTO userSessionDetails = CommonUtil.getUserSessionDetails(request);
			if (userSessionDetails == null) {
				responseJson.put("errorCode", "400");
				responseJson.put("errorMessage", "Bad Request");
				return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
			}
			return applyService.applyAllowAction(serviceId, taskId, userSessionDetails);
		} catch (Exception ex) {
			responseJson.put("errorCode", "500");
			responseJson.put("errorMessage", "Internal Server Error");
			return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/mvelDetails")
	public ResponseEntity<?> mvelDetails(@RequestParam Integer serviceId, HttpServletRequest request) {
		JSONObject responseJson = new JSONObject();
		try {
			UserSessionDTO userSessionDetails = CommonUtil.getUserSessionDetails(request);

			return applyService.mvelDetails(serviceId, userSessionDetails);
		} catch (Exception ex) {
			responseJson.put("errorCode", "500");
			responseJson.put("errorMessage", "Internal Server Error");
			return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/usedAttributeList")
	public ResponseEntity<?> getUsedAttr(@RequestParam Integer serviceId, @RequestParam String module,
			HttpServletRequest request) {
		JSONObject responseJson = new JSONObject();
		try {
			UserSessionDTO userSessionDetails = CommonUtil.getUserSessionDetails(request);

			return applyService.getUsedAttributeSet(serviceId, module, userSessionDetails);
		} catch (Exception ex) {
			responseJson.put("errorCode", "500");
			responseJson.put("errorMessage", "Internal Server Error");
			return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}