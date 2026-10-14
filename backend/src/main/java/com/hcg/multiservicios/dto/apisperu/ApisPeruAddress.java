package com.hcg.multiservicios.dto.apisperu;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApisPeruAddress {
    private String ubigeo;
    private String codigoPais = "PE";
    private String departamento;
    private String provincia;
    private String distrito;
    private String urbanizacion;
    private String direccion;
    private String codLocal;

    public ApisPeruAddress() {}

    public ApisPeruAddress(String direccion) {
        this.direccion = direccion;
    }

    public String getUbigeo() { return ubigeo; }
    public void setUbigeo(String ubigeo) { this.ubigeo = ubigeo; }

    public String getCodigoPais() { return codigoPais; }
    public void setCodigoPais(String codigoPais) { this.codigoPais = codigoPais; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }

    public String getDistrito() { return distrito; }
    public void setDistrito(String distrito) { this.distrito = distrito; }

    public String getUrbanizacion() { return urbanizacion; }
    public void setUrbanizacion(String urbanizacion) { this.urbanizacion = urbanizacion; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCodLocal() { return codLocal; }
    public void setCodLocal(String codLocal) { this.codLocal = codLocal; }
}
