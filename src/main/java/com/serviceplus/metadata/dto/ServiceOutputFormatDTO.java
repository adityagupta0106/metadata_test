package com.serviceplus.metadata.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ServiceOutputFormatDTO {

	private Integer serviceId;
	
	private List<String> tabList;
	
	private Long outputFormatId;
	
	@NotNull(message = "Template type cannot be empty")
	private Integer templateType;
	
	@NotEmpty(message = "Template name cannot be empty")
	private String templateName;
	
	//@NotNull(message = "Document type cannot be empty")
	private String documentType;
	
	private Integer layout;
	
	private String pageSize;
	
	private String waterMark;
	
	private String waterMarkOpacity;
	
	private Integer pdfMargins;
	
	private CustomMargin customMargin;
	
	private String templateBodySrc;
	
	private List<String> usedAttr;
	
	private String xsd;
	
	private List<JsonBuilder> jsonBuilder;
	
	private String xmlBuilder;
	
	private String notificationType;
	
	private String mailSubject;
	
	private String smsTempId;
	
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public static class JsonBuilder{
		private String name;
		private String parent;
		private String type;
		private String dynamicValue;
		private String constantValue;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getParent() {
			return parent;
		}
		public void setParent(String parent) {
			this.parent = parent;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getDynamicValue() {
			return dynamicValue;
		}
		public void setDynamicValue(String dynamicValue) {
			this.dynamicValue = dynamicValue;
		}
		public String getConstantValue() {
			return constantValue;
		}
		public void setConstantValue(String constantValue) {
			this.constantValue = constantValue;
		}
	}
	
	public Integer getServiceId() {
		return serviceId;
	}

	public void setServiceId(Integer serviceId) {
		this.serviceId = serviceId;
	}

	public List<String> getTabList() {
		return tabList;
	}

	public void setTabList(List<String> tabList) {
		this.tabList = tabList;
	}

	public Long getOutputFormatId() {
		return outputFormatId;
	}

	public void setOutputFormatId(Long outputFormatId) {
		this.outputFormatId = outputFormatId;
	}

	public Integer getTemplateType() {
		return templateType;
	}

	public void setTemplateType(Integer templateType) {
		this.templateType = templateType;
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public String getDocumentType() {
		return documentType;
	}

	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

	public Integer getLayout() {
		return layout;
	}

	public void setLayout(Integer layout) {
		this.layout = layout;
	}

	public String getPageSize() {
		return pageSize;
	}

	public void setPageSize(String pageSize) {
		this.pageSize = pageSize;
	}

	public String getWaterMark() {
		return waterMark;
	}

	public void setWaterMark(String waterMark) {
		this.waterMark = waterMark;
	}

	public String getWaterMarkOpacity() {
		return waterMarkOpacity;
	}

	public void setWaterMarkOpacity(String waterMarkOpacity) {
		this.waterMarkOpacity = waterMarkOpacity;
	}

	public Integer getPdfMargins() {
		return pdfMargins;
	}

	public void setPdfMargins(Integer pdfMargins) {
		this.pdfMargins = pdfMargins;
	}

	public CustomMargin getCustomMargin() {
		return customMargin;
	}

	public void setCustomMargin(CustomMargin customMargin) {
		this.customMargin = customMargin;
	}

	public String getTemplateBodySrc() {
		return templateBodySrc;
	}

	public void setTemplateBodySrc(String templateBodySrc) {
		this.templateBodySrc = templateBodySrc;
	}

	public List<String> getUsedAttr() {
		return usedAttr;
	}

	public void setUsedAttr(List<String> usedAttr) {
		this.usedAttr = usedAttr;
	}

	public String getXsd() {
		return xsd;
	}

	public void setXsd(String xsd) {
		this.xsd = xsd;
	}

	public List<JsonBuilder> getJsonBuilder() {
		return jsonBuilder;
	}

	public void setJsonBuilder(List<JsonBuilder> jsonBuilder) {
		this.jsonBuilder = jsonBuilder;
	}

	public String getXmlBuilder() {
		return xmlBuilder;
	}

	public void setXmlBuilder(String xmlBuilder) {
		this.xmlBuilder = xmlBuilder;
	}

	public String getNotificationType() {
		return notificationType;
	}

	public void setNotificationType(String notificationType) {
		this.notificationType = notificationType;
	}

	public String getMailSubject() {
		return mailSubject;
	}

	public void setMailSubject(String mailSubject) {
		this.mailSubject = mailSubject;
	}

	public String getSmsTempId() {
		return smsTempId;
	}

	public void setSmsTempId(String smsTempId) {
		this.smsTempId = smsTempId;
	}

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public static class CustomMargin {
		private String top;
		private String bottom;
		private String left;
		private String right;

		public String getTop() {
			return top;
		}

		public void setTop(String top) {
			this.top = top;
		}

		public String getBottom() {
			return bottom;
		}

		public void setBottom(String bottom) {
			this.bottom = bottom;
		}

		public String getLeft() {
			return left;
		}

		public void setLeft(String left) {
			this.left = left;
		}

		public String getRight() {
			return right;
		}

		public void setRight(String right) {
			this.right = right;
		}
	}
}
