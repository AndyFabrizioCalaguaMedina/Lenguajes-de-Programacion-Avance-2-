package com.hcg.multiservicios.dto.apisperu;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApisPeruSunatResponse {
    private Boolean success;
    private String cdrZip;
    private ApisPeruCdrResponse cdrResponse;
    private ApisPeruError error;

    public ApisPeruSunatResponse() {}

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public String getCdrZip() { return cdrZip; }
    public void setCdrZip(String cdrZip) { this.cdrZip = cdrZip; }

    public ApisPeruCdrResponse getCdrResponse() { return cdrResponse; }
    public void setCdrResponse(ApisPeruCdrResponse cdrResponse) { this.cdrResponse = cdrResponse; }

    public ApisPeruError getError() { return error; }
    public void setError(ApisPeruError error) { this.error = error; }
}
