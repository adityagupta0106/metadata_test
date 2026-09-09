package com.serviceplus.metadata.payment.dto;

import java.io.Serializable;
import java.util.List;

public class ApplicationAttributesResDTO implements Serializable {

	private static final long serialVersionUID = 1L;
	private Integer serviceId;
	private List<ApplAttribute> attributes;
	
	public static class ApplAttribute {
		private String attributeId;
		private String attributeLabel;
		private Integer typeId;
		
		public ApplAttribute(String attributeId, String attributeLabel, Integer typeId) {
			super();
			this.attributeId = attributeId;
			this.attributeLabel = attributeLabel;
			this.typeId = typeId;
		}
		public String getAttributeId() {
			return attributeId;
		}
		public void setAttributeId(String attributeId) {
			this.attributeId = attributeId;
		}
		public String getAttributeLabel() {
			return attributeLabel;
		}
		public void setAttributeLabel(String attributeLabel) {
			this.attributeLabel = attributeLabel;
		}
		public Integer getTypeId() {
			return typeId;
		}
		public void setTypeId(Integer typeId) {
			this.typeId = typeId;
		}
	}
	public Integer getServiceId() {
		return serviceId;
	}
	public void setServiceId(Integer serviceId) {
		this.serviceId = serviceId;
	}
	public List<ApplAttribute> getAttributes() {
		return attributes;
	}
	public void setAttributes(List<ApplAttribute> attributes) {
		this.attributes = attributes;
	}
	
}
