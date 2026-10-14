package com.hcg.multiservicios.dto.apisperu;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApisPeruInvoiceResponse {
    private String hash;
    private String xml;
    private ApisPeruSunatResponse sunatResponse;
    private String message;
    private String field;
    private Object error;

    public ApisPeruInvoiceResponse() {}

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public String getXml() { return xml; }
    public void setXml(String xml) { this.xml = xml; }

    public ApisPeruSunatResponse getSunatResponse() { return sunatResponse; }
    public void setSunatResponse(ApisPeruSunatResponse sunatResponse) { this.sunatResponse = sunatResponse; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public Object getError() { return error; }
    public void setError(Object error) { this.error = error; }
}
