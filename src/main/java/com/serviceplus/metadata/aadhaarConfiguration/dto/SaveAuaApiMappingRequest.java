package com.serviceplus.metadata.aadhaarConfiguration.dto;

import java.util.List;

public class SaveAuaApiMappingRequest {

    private List<SaveAuaFieldMappingRequest> mappings;

    public List<SaveAuaFieldMappingRequest> getMappings() {
        return mappings;
    }

    public void setMappings(List<SaveAuaFieldMappingRequest> mappings) {
        this.mappings = mappings;
    }
}
