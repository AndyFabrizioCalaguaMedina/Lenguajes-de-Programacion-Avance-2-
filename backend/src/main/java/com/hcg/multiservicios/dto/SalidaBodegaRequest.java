package com.hcg.multiservicios.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * destinoTipo PROVEEDOR requiere proveedorId; CLIENTE requiere clienteId; OTRO requiere destinoNombre.
 * Cada item lleva productoId o codigo (el código leído con el escáner/cámara del celular).
 */
public record SalidaBodegaRequest(
        @NotBlank(message = "Indique el destino")
        @Pattern(regexp = "PROVEEDOR|CLIENTE|OTRO", message = "Destino no válido (PROVEEDOR, CLIENTE u OTRO)") String destinoTipo,
        Integer proveedorId,
        Integer clienteId,
        @Size(max = 255) String destinoNombre,
        @NotBlank(message = "Indique el motivo")
        @Pattern(regexp = "ENVIO|DEVOLUCION|VENTA|MERMA|OTRO", message = "Motivo no válido (ENVIO, DEVOLUCION, VENTA, MERMA u OTRO)") String motivo,
        @Size(max = 500) String observacion,
        @NotEmpty(message = "Agregue al menos un producto") @Valid List<Item> items) {

    public record Item(
            Integer productoId,
            @Size(max = 30) String codigo,
            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser al menos 1") Integer cantidad) {
    }
}
