package com.hcg.multiservicios.dto.apisperu;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemBoletaDto {
    @JsonAlias({"codigo", "id"})
    private String codProducto = "PRD-01";

    @JsonAlias({"nombre", "producto", "concepto"})
    @jakarta.validation.constraints.NotBlank(message = "Indique la descripción")
    @jakarta.validation.constraints.Size(max = 500)
    private String descripcion;

    private String unidad = "NIU";

    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.DecimalMin("0.01")
    @jakarta.validation.constraints.Digits(integer = 10, fraction = 2)
    private BigDecimal cantidad = BigDecimal.ONE;

    @JsonAlias({"precio", "precioVenta", "mtoPrecioUnitario"})
    @jakarta.validation.constraints.DecimalMin("0.00")
    @jakarta.validation.constraints.Digits(integer = 10, fraction = 2)
    private BigDecimal precioUnitario;

    @JsonAlias({"mtoValorUnitario", "valor"})
    @jakarta.validation.constraints.DecimalMin("0.00")
    @jakarta.validation.constraints.Digits(integer = 10, fraction = 2)
    private BigDecimal valorUnitario;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.validation.constraints.AssertTrue(message = "Indique precioUnitario o valorUnitario")
    public boolean isPrecioDefinido() {
        return precioUnitario != null || valorUnitario != null;
    }

    public ItemBoletaDto() {}

    public ItemBoletaDto(String codProducto, String descripcion, BigDecimal cantidad, BigDecimal precioUnitario) {
        this.codProducto = codProducto;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public String getCodProducto() { return codProducto; }
    public void setCodProducto(String codProducto) { this.codProducto = codProducto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
}

