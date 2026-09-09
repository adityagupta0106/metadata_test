package com.serviceplus.metadata.payment.dto;

public class PaymentConfigDetailsDTO {
			private String parameterId;
			private String parameterType;
			private String filePath;
			private String displayName;
			private Boolean parameterRequired;
			private String attrInputType;
			private String dynamic;
			private Boolean parameterForRefund;
			private String defaultValue;
			public String getParameterId() {
				return parameterId;
			}
			public void setParameterId(String parameterId) {
				this.parameterId = parameterId;
			}
			public String getParameterType() {
				return parameterType;
			}
			public void setParameterType(String parameterType) {
				this.parameterType = parameterType;
			}
			public String getFilePath() {
				return filePath;
			}
			public void setFilePath(String filePath) {
				this.filePath = filePath;
			}
			public String getDisplayName() {
				return displayName;
			}
			public void setDisplayName(String displayName) {
				this.displayName = displayName;
			}
			public Boolean getParameterRequired() {
				return parameterRequired;
			}
			public void setParameterRequired(Boolean parameterRequired) {
				this.parameterRequired = parameterRequired;
			}
			public String getAttrInputType() {
				return attrInputType;
			}
			public void setAttrInputType(String attrInputType) {
				this.attrInputType = attrInputType;
			}
			public String getDynamic() {
				return dynamic;
			}
			public void setDynamic(String dynamic) {
				this.dynamic = dynamic;
			}
			public Boolean getParameterForRefund() {
				return parameterForRefund;
			}
			public void setParameterForRefund(Boolean parameterForRefund) {
				this.parameterForRefund = parameterForRefund;
			}
			public String getDefaultValue() {
				return defaultValue;
			}
			public void setDefaultValue(String defaultValue) {
				this.defaultValue = defaultValue;
			}
			
}
