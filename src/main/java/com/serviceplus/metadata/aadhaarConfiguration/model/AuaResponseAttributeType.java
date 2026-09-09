package com.serviceplus.metadata.aadhaarConfiguration.model;

public enum AuaResponseAttributeType {

    RESULT("Result");

    private final String label;

    AuaResponseAttributeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
