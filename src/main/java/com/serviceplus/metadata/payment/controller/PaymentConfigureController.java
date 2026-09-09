package com.serviceplus.metadata.payment.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.serviceplus.metadata.dto.ServiceChargeDTO;
import com.serviceplus.metadata.dto.UserSessionDTO;
import com.serviceplus.metadata.payment.service.PaymentConfigureService;
import com.serviceplus.metadata.utility.CommonUtil;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class PaymentConfigureController {

	@Autowired
	private PaymentConfigureService paymentConfigureService;
	
	@GetMapping("/paymentConfig")
   	public ResponseEntity<?> getChargeDetailList(@RequestParam Integer serviceId,HttpServletRequest request) {
   		JSONObject responseJson = new JSONObject();

   		try {
   			UserSessionDTO userSessionDetails = CommonUtil.getUserSessionDetails(request);
   			userSessionDetails=getLocalSessionDetails(userSessionDetails);
   	    	ResponseEntity<?> tokenValidationResult = CommonUtil.validateServiceToken(request, serviceId, responseJson);
   	        if (tokenValidationResult != null) {
   	            return tokenValidationResult;
   	        }
   			if (userSessionDetails == null) {
   				responseJson.put("errorCode", "400");
   				responseJson.put("errorMessage", "Bad Request");
   				return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
   			}
   			return paymentConfigureService.getChargeDetailList(serviceId,userSessionDetails);

   		} catch (Exception ex) {
   			ex.printStackTrace();
   			responseJson.put("errorCode", "500");
   			responseJson.put("errorMessage", "Internal Server Error");
   			return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
   		}

   	}
	
	@PostMapping("/paymentConfig")
	 public ResponseEntity<?> getPaymentConfig(@RequestBody ServiceChargeDTO serviceCharge,HttpServletRequest request){
		JSONObject responseJson= new JSONObject();
		try {
			UserSessionDTO userSessionDetails = CommonUtil.getUserSessionDetails(request);
			userSessionDetails=getLocalSessionDetails(userSessionDetails);
	    	ResponseEntity<?> tokenValidationResult = CommonUtil.validateServiceToken(request, serviceCharge.getServiceId(), responseJson);
	        if (tokenValidationResult != null) {
	            return tokenValidationResult;
	        }
			if (userSessionDetails == null) {
	        	responseJson.put("errorCode", "400");
	        	responseJson.put("errorMessage", "Bad Request");
	            return new ResponseEntity<>(responseJson, HttpStatus.BAD_REQUEST);
				
			}
			return paymentConfigureService.getPaymentConfig(serviceCharge,request,userSessionDetails);
		}catch(Exception ex) {
			ex.printStackTrace();
    		responseJson.put("errorCode", "500");
        	responseJson.put("errorMessage", "Internal Server Error");
            return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    	}
	}
	
	private UserSessionDTO getLocalSessionDetails(UserSessionDTO userSessionDetails) {
		/*
		 * userSessionDetails=new UserSessionDTO();
		 * userSessionDetails.setUserID(227175); userSessionDetails.setStateId(32);
		 * userSessionDetails.setDepartmentId(544);
		 */
		return userSessionDetails;
	} 
}
