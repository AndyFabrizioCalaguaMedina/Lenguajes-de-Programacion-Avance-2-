package com.hcg.multiservicios.dto.apisperu;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApisPeruFormaPago {
    private String moneda = "PEN";
    private String tipo = "Contado";

    public ApisPeruFormaPago() {}

    public ApisPeruFormaPago(String tipo, String moneda) {
        this.tipo = tipo;
        this.moneda = moneda;
    }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}
