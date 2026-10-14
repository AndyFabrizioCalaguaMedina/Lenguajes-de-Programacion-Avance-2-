package com.hcg.multiservicios.dto.apisperu;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApisPeruLegend {
    private String code = "1000";
    private String value;

    public ApisPeruLegend() {}

    public ApisPeruLegend(String code, String value) {
        this.code = code;
        this.value = value;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
