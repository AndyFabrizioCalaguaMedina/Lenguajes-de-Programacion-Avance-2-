package com.hcg.multiservicios.dto.apisperu;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApisPeruCompany {
    private String ruc;
    private String razonSocial;
    private String nombreComercial;
    private ApisPeruAddress address;
    private String email;
    private String telephone;

    public ApisPeruCompany() {}

    public ApisPeruCompany(String ruc, String razonSocial) {
        this.ruc = ruc;
        this.razonSocial = razonSocial;
    }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getNombreComercial() { return nombreComercial; }
    public void setNombreComercial(String nombreComercial) { this.nombreComercial = nombreComercial; }

    public ApisPeruAddress getAddress() { return address; }
    public void setAddress(ApisPeruAddress address) { this.address = address; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
}
