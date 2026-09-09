package com.serviceplus.metadata.payment.dto;

import java.util.List;

import com.serviceplus.metadata.payment.entity.PaymentConfiguration;

public class PaymentConfigResponseDTO {
	private List<PaymentConfigDetailsDTO> configDetails;
	private PaymentMode paymentMode;

	public List<PaymentConfigDetailsDTO> getPaymentConfiguration() {
		return configDetails;
	}

	public void setPaymentConfiguration(PaymentConfiguration paymentConfiguration) {
		this.configDetails = paymentConfiguration.getPaymentConfigDetails();
	}

	public PaymentMode getPaymentMode() {
		return paymentMode;
	}

	public void setPaymentMode(PaymentModeMasterDTO paymentModeMast) {
		this.paymentMode = new PaymentMode();
		paymentMode.setValue(paymentModeMast.getValue());
		paymentMode.setLabel(paymentModeMast.getLabel());
		paymentMode.setDefaceEnable(paymentModeMast.getDefaceEnable());
		paymentMode.setDvEnable(paymentModeMast.getDvEnable());
	}

	public static class PaymentMode {
		private Integer value;
		private String label;
		private Boolean dvEnable;
		private Boolean defaceEnable;

		public Integer getValue() {
			return value;
		}

		public void setValue(Integer value) {
			this.value = value;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public Boolean getDefaceEnable() {
			return defaceEnable;
		}

		public void setDefaceEnable(Boolean defaceEnable) {
			this.defaceEnable = defaceEnable;
		}

		public Boolean getDvEnable() {
			return dvEnable;
		}

		public void setDvEnable(Boolean dvEnable) {
			this.dvEnable = dvEnable;
		}

	}

}
