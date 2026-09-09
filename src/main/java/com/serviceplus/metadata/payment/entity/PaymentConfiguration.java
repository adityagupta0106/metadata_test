package com.serviceplus.metadata.payment.entity;

import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.serviceplus.metadata.payment.dto.PaymentConfigDetailsDTO;
import com.serviceplus.metadata.utility.ApplicationConstants;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_configuration_master", schema = ApplicationConstants.SP_SCHEMA_NAME)
public class PaymentConfiguration {
	
	@Id
	@Column(name = "payment_option_id", nullable = false)
	private Integer paymentOptionId;

	@Column(name = "payment_config_details", columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private List<PaymentConfigDetailsDTO> paymentConfigDetails;

	public Integer getPaymentOptionId() {
		return paymentOptionId;
	}

	public void setPaymentOptionId(Integer paymentOptionId) {
		this.paymentOptionId = paymentOptionId;
	}

	public List<PaymentConfigDetailsDTO> getPaymentConfigDetails() {
		return paymentConfigDetails;
	}

	public void setPaymentConfigDetails(List<PaymentConfigDetailsDTO> paymentConfigDetails) {
		this.paymentConfigDetails = paymentConfigDetails;
	}

}
