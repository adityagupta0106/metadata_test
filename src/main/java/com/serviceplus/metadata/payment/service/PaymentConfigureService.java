package com.serviceplus.metadata.payment.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceplus.metadata.dto.ServiceChargeDTO;
import com.serviceplus.metadata.dto.ServiceChargeDTO.PaymentModeDTO;
import com.serviceplus.metadata.dto.UserSessionDTO;
import com.serviceplus.metadata.entity.ServiceChargeDetail;
import com.serviceplus.metadata.entity.ServiceMasterData;
import com.serviceplus.metadata.payment.dto.PaymentConfigResponseDTO;
import com.serviceplus.metadata.payment.dto.PaymentModeMasterDTO;
import com.serviceplus.metadata.payment.entity.PaymentConfiguration;
import com.serviceplus.metadata.payment.repository.PaymentConfigurationRepository;
import com.serviceplus.metadata.repository.IServiceChargeRepository;
import com.serviceplus.metadata.repository.IServiceMasterDataRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class PaymentConfigureService {
	@Autowired
	private IServiceChargeRepository srvChargeRepo;
	@Autowired
	private PaymentConfigurationRepository payConfigRepo;
	@Autowired
	private IServiceMasterDataRepository masterDataRepository;

	public ResponseEntity<?> getPaymentConfig(ServiceChargeDTO serviceCharge, HttpServletRequest request, UserSessionDTO userSessionDetails) {
		JSONObject responseJson = new JSONObject();
		try {
			List<ServiceChargeDetail> serviceChargeDetailList = srvChargeRepo
					.findByServiceIdAndUserId(serviceCharge.getServiceId(), userSessionDetails.getUserID().longValue());

			if (serviceChargeDetailList.isEmpty()) {
				responseJson.put("errorCode", "404");
				responseJson.put("errorMessage", "Invalid Payment Configuration Request.");
				return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
			}
			if (serviceCharge.getPaymentModes() == null || serviceCharge.getPaymentModes().isEmpty()) {
				responseJson.put("errorCode", "404");
				responseJson.put("errorMessage", "Payment Mode is required.");
				return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
			}

			PaymentModeDTO requestedPaymentMode = serviceCharge.getPaymentModes().get(0);
			Integer reqPayValue = requestedPaymentMode.getValue();
			if (reqPayValue == null) {
				responseJson.put("errorCode", "404");
				responseJson.put("errorMessage", "Invalid Payment Mode.");
				return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
			}
			boolean isConfigurable = false;
			Integer dbPayValue = null;

			for (ServiceChargeDetail dbCrgDetails : serviceChargeDetailList) {
				ServiceChargeDTO dbServiceCharge = dbCrgDetails.getServiceChargeJson();
				if (dbServiceCharge == null || dbServiceCharge.getPaymentModes() == null) {
					continue;
				}

				for (PaymentModeDTO dbPaymentMode : dbServiceCharge.getPaymentModes()) {

					if (dbPaymentMode == null || dbPaymentMode.getValue() == null) {
						continue;
					}

					if (reqPayValue.equals(dbPaymentMode.getValue()) && Boolean.TRUE.equals(dbPaymentMode.getOnlineFlag())) {
						isConfigurable = true;
						dbPayValue = dbPaymentMode.getValue();
						break;
					}
				}

				if (isConfigurable) {
					break;
				}
			}

			if (!isConfigurable) {
				responseJson.put("errorMessage", "Invalid Payment Mode Details");
				responseJson.put("errorCode", "404");

				return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
			}
			ServiceMasterData masterData = masterDataRepository.findByName("Payment Mode")
					.orElseThrow(() -> new RuntimeException("Payment Mode not found"));

			ObjectMapper mapper = new ObjectMapper();

			List<PaymentModeMasterDTO> paymentModeMastList = mapper.readValue(masterData.getData(),
					new TypeReference<List<PaymentModeMasterDTO>>() {
					});

			PaymentModeMasterDTO paymentModeMast = null;

			for (PaymentModeMasterDTO payMode : paymentModeMastList) {

				if (payMode.getValue() != null && payMode.getValue().equals(dbPayValue)) {
					paymentModeMast = payMode;
					break;
				}
			}

			if (paymentModeMast == null) {
				responseJson.put("errorMessage", "Invalid Payment Mode Details");
				responseJson.put("errorCode", "404");
				return new ResponseEntity<>(responseJson.toString(), HttpStatus.BAD_REQUEST);
			}
			PaymentConfiguration paymentConfiguration = payConfigRepo.findByPaymentOptionId(dbPayValue);

			if (paymentConfiguration == null) {
				paymentConfiguration = new PaymentConfiguration();
			}

			PaymentConfigResponseDTO paymentConfigResponseDTO = setPaymentConfigResponse(null, paymentConfiguration, paymentModeMast);
			return ResponseEntity.ok(paymentConfigResponseDTO);

		} catch (Exception ex) {
			ex.printStackTrace();
			responseJson.put("errorCode", "500");
			responseJson.put("errorMessage", "Internal Server Error");
			return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public ResponseEntity<?> getChargeDetailList(Integer serviceId, UserSessionDTO userSessionDetails) {

		JSONObject responseJson = new JSONObject();
		try {
			List<ServiceChargeDetail> serviceChargeDetailList = srvChargeRepo.findByServiceIdAndUserId(serviceId,
					userSessionDetails.getUserID().longValue());
			Map<Integer, PaymentModeDTO> paymentModeMap = new HashMap<>();
			for (ServiceChargeDetail serviceChargeDetail : serviceChargeDetailList) {
				ServiceChargeDTO serviceChargeDTO = serviceChargeDetail.getServiceChargeJson();
				if (serviceChargeDTO == null || serviceChargeDTO.getPaymentModes() == null) {
					continue;
				}
				for (PaymentModeDTO paymentMode : serviceChargeDTO.getPaymentModes()) {
					if (Boolean.TRUE.equals(paymentMode.getOnlineFlag())) {
						paymentModeMap.put(paymentMode.getValue(), paymentMode);
					}
				}
			}
			return ResponseEntity.ok(paymentModeMap.values());
		} catch (Exception ex) {
			ex.printStackTrace();
			responseJson.put("errorCode", "500");
			responseJson.put("errorMessage", "Internal Server Error");
			return new ResponseEntity<>(responseJson.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	private PaymentConfigResponseDTO setPaymentConfigResponse(PaymentConfigResponseDTO paymentConfigResponse,PaymentConfiguration paymentConfiguration, PaymentModeMasterDTO paymentModeMast){
		paymentConfigResponse = new PaymentConfigResponseDTO();
		paymentConfigResponse.setPaymentConfiguration(paymentConfiguration);
		paymentConfigResponse.setPaymentMode(paymentModeMast);
		return paymentConfigResponse;
	}
}
