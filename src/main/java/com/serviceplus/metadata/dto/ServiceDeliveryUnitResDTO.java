package com.serviceplus.metadata.dto;

import java.util.List;

import com.serviceplus.metadata.dto.ServiceDeliveryUnitDTO.ServiceDeliveryUnitJSON;

public class ServiceDeliveryUnitResDTO {
	private Integer serviceId;
	private String sduSource;
	private List<ServiceDeliveryUnitJSON> serviceDeliveryUnits;
	private String tablist;

	public Integer getServiceId() {
		return serviceId;
	}

	public void setServiceId(Integer serviceId) {
		this.serviceId = serviceId;
	}

	public String getSduSource() {
		return sduSource;
	}

	public void setSduSource(String sduSource) {
		this.sduSource = sduSource;
	}

	public List<ServiceDeliveryUnitJSON> getServiceDeliveryUnits() {
		return serviceDeliveryUnits;
	}

	public void setServiceDeliveryUnits(List<ServiceDeliveryUnitJSON> serviceDeliveryUnits) {
		this.serviceDeliveryUnits = serviceDeliveryUnits;
	}

	public String getTablist() {
		return tablist;
	}

	public void setTablist(String tablist) {
		this.tablist = tablist;
	}

}
