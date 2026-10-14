package com.hcg.multiservicios.dto.apisperu;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApisPeruDetailItem {
    private String codProducto;
    private String unidad = "NIU";
    private String descripcion;
    private BigDecimal cantidad;
    private BigDecimal mtoValorUnitario;
    private BigDecimal mtoPrecioUnitario;
    private BigDecimal mtoValorVenta;
    private BigDecimal mtoBaseIgv;
    private BigDecimal porcentajeIgv = BigDecimal.valueOf(18.0);
    private BigDecimal igv;
    private String tipAfeIgv = "10";
    private BigDecimal totalImpuestos;

    public ApisPeruDetailItem() {}

    public String getCodProducto() { return codProducto; }
    public void setCodProducto(String codProducto) { this.codProducto = codProducto; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }

    public BigDecimal getMtoValorUnitario() { return mtoValorUnitario; }
    public void setMtoValorUnitario(BigDecimal mtoValorUnitario) { this.mtoValorUnitario = mtoValorUnitario; }

    public BigDecimal getMtoPrecioUnitario() { return mtoPrecioUnitario; }
    public void setMtoPrecioUnitario(BigDecimal mtoPrecioUnitario) { this.mtoPrecioUnitario = mtoPrecioUnitario; }

    public BigDecimal getMtoValorVenta() { return mtoValorVenta; }
    public void setMtoValorVenta(BigDecimal mtoValorVenta) { this.mtoValorVenta = mtoValorVenta; }

    public BigDecimal getMtoBaseIgv() { return mtoBaseIgv; }
    public void setMtoBaseIgv(BigDecimal mtoBaseIgv) { this.mtoBaseIgv = mtoBaseIgv; }

    public BigDecimal getPorcentajeIgv() { return porcentajeIgv; }
    public void setPorcentajeIgv(BigDecimal porcentajeIgv) { this.porcentajeIgv = porcentajeIgv; }

    public BigDecimal getIgv() { return igv; }
    public void setIgv(BigDecimal igv) { this.igv = igv; }

    public String getTipAfeIgv() { return tipAfeIgv; }
    public void setTipAfeIgv(String tipAfeIgv) { this.tipAfeIgv = tipAfeIgv; }

    public BigDecimal getTotalImpuestos() { return totalImpuestos; }
    public void setTotalImpuestos(BigDecimal totalImpuestos) { this.totalImpuestos = totalImpuestos; }
}
