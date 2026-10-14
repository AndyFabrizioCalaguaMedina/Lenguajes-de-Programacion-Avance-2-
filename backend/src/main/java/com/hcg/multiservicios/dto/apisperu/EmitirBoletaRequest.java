package com.hcg.multiservicios.dto.apisperu;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmitirBoletaRequest {
    private String tipoDoc = "03"; // 03 = Boleta de Venta
    private String serie; // si no viene, el servidor usa BBB1 (boleta) o FFF1 (factura)
    private String correlativo;
    private String fechaEmision;
    private String tipoMoneda = "PEN";

    @JsonAlias({"client"})
    private ClienteBoletaDto cliente;

    @JsonAlias({"detalles", "details", "productos"})
    @jakarta.validation.constraints.NotEmpty(message = "Agregue al menos un ítem")
    private List<@jakarta.validation.constraints.NotNull @jakarta.validation.Valid ItemBoletaDto> items = new ArrayList<>();

    private String observacion;

    // Campos opcionales si el frontend ya envía los totales calculados
    private BigDecimal mtoOperGravadas;
    private BigDecimal mtoIGV;
    private BigDecimal mtoImpVenta;

    public EmitirBoletaRequest() {}

    public String getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }

    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }

    public String getCorrelativo() { return correlativo; }
    public void setCorrelativo(String correlativo) { this.correlativo = correlativo; }

    public String getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(String fechaEmision) { this.fechaEmision = fechaEmision; }

    public String getTipoMoneda() { return tipoMoneda; }
    public void setTipoMoneda(String tipoMoneda) { this.tipoMoneda = tipoMoneda; }

    public ClienteBoletaDto getCliente() { return cliente; }
    public void setCliente(ClienteBoletaDto cliente) { this.cliente = cliente; }

    public List<ItemBoletaDto> getItems() { return items; }
    public void setItems(List<ItemBoletaDto> items) { this.items = items; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public BigDecimal getMtoOperGravadas() { return mtoOperGravadas; }
    public void setMtoOperGravadas(BigDecimal mtoOperGravadas) { this.mtoOperGravadas = mtoOperGravadas; }

    public BigDecimal getMtoIGV() { return mtoIGV; }
    public void setMtoIGV(BigDecimal mtoIGV) { this.mtoIGV = mtoIGV; }

    public BigDecimal getMtoImpVenta() { return mtoImpVenta; }
    public void setMtoImpVenta(BigDecimal mtoImpVenta) { this.mtoImpVenta = mtoImpVenta; }
}

