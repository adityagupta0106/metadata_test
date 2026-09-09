package com.serviceplus.metadata.payment.dto;

public class PaymentModeMasterDTO {
	private String type;
	private String label;
	private Integer value;
	private Boolean online;
	private Boolean dvEnable;
	private Boolean defaceEnable;
	private Integer definedStateCode;
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public Integer getValue() {
		return value;
	}
	public void setValue(Integer value) {
		this.value = value;
	}
	public Boolean getOnline() {
		return online;
	}
	public void setOnline(Boolean online) {
		this.online = online;
	}
	public Boolean getDvEnable() {
		return dvEnable;
	}
	public void setDvEnable(Boolean dvEnable) {
		this.dvEnable = dvEnable;
	}
	public Boolean getDefaceEnable() {
		return defaceEnable;
	}
	public void setDefaceEnable(Boolean defaceEnable) {
		this.defaceEnable = defaceEnable;
	}
	public Integer getDefinedStateCode() {
		return definedStateCode;
	}
	public void setDefinedStateCode(Integer definedStateCode) {
		this.definedStateCode = definedStateCode;
	}
	

}
