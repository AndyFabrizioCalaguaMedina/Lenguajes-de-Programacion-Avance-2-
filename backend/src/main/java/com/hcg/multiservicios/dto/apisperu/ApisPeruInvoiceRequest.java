package com.hcg.multiservicios.dto.apisperu;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApisPeruInvoiceRequest {
    private String ublVersion = "2.1";
    private String tipoOperacion = "0101";
    private String tipoDoc = "03"; // 03 = Boleta de Venta
    private String serie = "BBB1";
    private String correlativo;
    private String fechaEmision;
    private ApisPeruFormaPago formaPago = new ApisPeruFormaPago("Contado", "PEN");
    private String tipoMoneda = "PEN";
    private ApisPeruCompany company;
    private ApisPeruClient client;
    private BigDecimal mtoOperGravadas;
    private BigDecimal mtoIGV;
    private BigDecimal totalImpuestos;
    private BigDecimal valorVenta;
    private BigDecimal subTotal;
    private BigDecimal mtoImpVenta;
    private List<ApisPeruDetailItem> details = new ArrayList<>();
    private List<ApisPeruLegend> legends = new ArrayList<>();
    private String observacion;

    public ApisPeruInvoiceRequest() {}

    public String getUblVersion() { return ublVersion; }
    public void setUblVersion(String ublVersion) { this.ublVersion = ublVersion; }

    public String getTipoOperacion() { return tipoOperacion; }
    public void setTipoOperacion(String tipoOperacion) { this.tipoOperacion = tipoOperacion; }

    public String getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }

    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }

    public String getCorrelativo() { return correlativo; }
    public void setCorrelativo(String correlativo) { this.correlativo = correlativo; }

    public String getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(String fechaEmision) { this.fechaEmision = fechaEmision; }

    public ApisPeruFormaPago getFormaPago() { return formaPago; }
    public void setFormaPago(ApisPeruFormaPago formaPago) { this.formaPago = formaPago; }

    public String getTipoMoneda() { return tipoMoneda; }
    public void setTipoMoneda(String tipoMoneda) { this.tipoMoneda = tipoMoneda; }

    public ApisPeruCompany getCompany() { return company; }
    public void setCompany(ApisPeruCompany company) { this.company = company; }

    public ApisPeruClient getClient() { return client; }
    public void setClient(ApisPeruClient client) { this.client = client; }

    public BigDecimal getMtoOperGravadas() { return mtoOperGravadas; }
    public void setMtoOperGravadas(BigDecimal mtoOperGravadas) { this.mtoOperGravadas = mtoOperGravadas; }

    public BigDecimal getMtoIGV() { return mtoIGV; }
    public void setMtoIGV(BigDecimal mtoIGV) { this.mtoIGV = mtoIGV; }

    public BigDecimal getTotalImpuestos() { return totalImpuestos; }
    public void setTotalImpuestos(BigDecimal totalImpuestos) { this.totalImpuestos = totalImpuestos; }

    public BigDecimal getValorVenta() { return valorVenta; }
    public void setValorVenta(BigDecimal valorVenta) { this.valorVenta = valorVenta; }

    public BigDecimal getSubTotal() { return subTotal; }
    public void setSubTotal(BigDecimal subTotal) { this.subTotal = subTotal; }

    public BigDecimal getMtoImpVenta() { return mtoImpVenta; }
    public void setMtoImpVenta(BigDecimal mtoImpVenta) { this.mtoImpVenta = mtoImpVenta; }

    public List<ApisPeruDetailItem> getDetails() { return details; }
    public void setDetails(List<ApisPeruDetailItem> details) { this.details = details; }

    public List<ApisPeruLegend> getLegends() { return legends; }
    public void setLegends(List<ApisPeruLegend> legends) { this.legends = legends; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
