package com.hcg.multiservicios.dto.apisperu;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApisPeruClient {
    private String tipoDoc;
    private String numDoc;
    private String rznSocial;
    private ApisPeruAddress address;
    private String email;
    private String telephone;

    public ApisPeruClient() {}

    public ApisPeruClient(String tipoDoc, String numDoc, String rznSocial) {
        this.tipoDoc = tipoDoc;
        this.numDoc = numDoc;
        this.rznSocial = rznSocial;
    }

    public String getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }

    public String getNumDoc() { return numDoc; }
    public void setNumDoc(String numDoc) { this.numDoc = numDoc; }

    public String getRznSocial() { return rznSocial; }
    public void setRznSocial(String rznSocial) { this.rznSocial = rznSocial; }

    public ApisPeruAddress getAddress() { return address; }
    public void setAddress(ApisPeruAddress address) { this.address = address; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
}
