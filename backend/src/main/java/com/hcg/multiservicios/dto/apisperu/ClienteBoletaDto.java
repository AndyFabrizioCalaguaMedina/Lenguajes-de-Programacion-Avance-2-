package com.hcg.multiservicios.dto.apisperu;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClienteBoletaDto {
    private String tipoDoc = "1"; // Default 1: DNI

    @JsonAlias({"numeroDocumento", "dni", "ruc"})
    private String numDoc;

    @JsonAlias({"nombre", "razonSocial", "nombres"})
    private String rznSocial;

    private String direccion;
    private String email;
    private String telefono;

    public ClienteBoletaDto() {}

    public ClienteBoletaDto(String tipoDoc, String numDoc, String rznSocial) {
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

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}
