package com.serviceplus.metadata.aadhaarConfiguration.dto;

import java.util.List;

public class AuaMastersResponse {

    private List<MasterOptionResponse> auaTypes;
    private List<MasterOptionResponse> sourceTypes;
    private List<MasterOptionResponse> transformations;
    private List<MasterOptionResponse> responseAttributeTypes;

    public AuaMastersResponse() {
    }

    public List<MasterOptionResponse> getAuaTypes() {
        return auaTypes;
    }

    public void setAuaTypes(List<MasterOptionResponse> auaTypes) {
        this.auaTypes = auaTypes;
    }

    public List<MasterOptionResponse> getTransformations() {
        return transformations;
    }

    public void setTransformations(List<MasterOptionResponse> transformations) {
        this.transformations = transformations;
    }

    public List<MasterOptionResponse> getSourceTypes() {
        return sourceTypes;
    }

    public void setSourceTypes(List<MasterOptionResponse> sourceTypes) {
        this.sourceTypes = sourceTypes;
    }

    public List<MasterOptionResponse> getResponseAttributeTypes() {
        return responseAttributeTypes;
    }

    public void setResponseAttributeTypes(List<MasterOptionResponse> responseAttributeTypes) {
        this.responseAttributeTypes = responseAttributeTypes;
    }
}
