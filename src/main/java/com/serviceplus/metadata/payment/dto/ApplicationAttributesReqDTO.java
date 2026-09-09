package com.serviceplus.metadata.payment.dto;

import java.io.Serializable;

public class ApplicationAttributesReqDTO implements Serializable{
	private static final long serialVersionUID = 1L;
	private Integer serviceId;
	private String inputType;
	public Integer getServiceId() {
		return serviceId;
	}
	public void setServiceId(Integer serviceId) {
		this.serviceId = serviceId;
	}
	public String getInputType() {
		return inputType;
	}
	public void setInputType(String inputType) {
		this.inputType = inputType;
	}

}
