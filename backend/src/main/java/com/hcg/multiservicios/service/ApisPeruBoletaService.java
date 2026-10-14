package com.hcg.multiservicios.service;

import com.hcg.multiservicios.dto.apisperu.ApisPeruAddress;
import com.hcg.multiservicios.dto.apisperu.ApisPeruClient;
import com.hcg.multiservicios.dto.apisperu.ApisPeruCompany;
import com.hcg.multiservicios.dto.apisperu.ApisPeruDetailItem;
import com.hcg.multiservicios.dto.apisperu.ApisPeruError;
import com.hcg.multiservicios.dto.apisperu.ApisPeruInvoiceRequest;
import com.hcg.multiservicios.dto.apisperu.ApisPeruInvoiceResponse;
import com.hcg.multiservicios.dto.apisperu.ApisPeruLegend;
import com.hcg.multiservicios.dto.apisperu.ApisPeruSunatResponse;
import com.hcg.multiservicios.dto.apisperu.BoletaResponse;
import com.hcg.multiservicios.dto.apisperu.ClienteBoletaDto;
import com.hcg.multiservicios.dto.apisperu.EmitirBoletaRequest;
import com.hcg.multiservicios.dto.apisperu.ItemBoletaDto;
import com.hcg.multiservicios.util.NumeroALetras;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ApisPeruBoletaService {

    private static final Logger log = LoggerFactory.getLogger(ApisPeruBoletaService.class);
    private static final BigDecimal IGV_FACTOR = BigDecimal.valueOf(1.18);
    private static final BigDecimal IGV_TASA = BigDecimal.valueOf(0.18);
    private static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");

    private final String apisPeruUrl;
    private final String apisPeruToken;
    private final String apisPeruPdfUrl;
    private final String companyRuc;
    private final String companyRazonSocial;
    private final String companyDireccion;
    private final String companyUbigeo;
    private final RestClient restClient;

    public ApisPeruBoletaService(
            @Value("${apisperu.url:https://facturacion.apisperu.com/api/v1/invoice/send}") String apisPeruUrl,
            @Value("${apisperu.token}") String apisPeruToken,
            @Value("${apisperu.pdf-url:}") String pdfUrlConfigurada,
            @Value("${apisperu.company.ruc:20026788123}") String companyRuc,
            @Value("${apisperu.company.razon-social:HCG MULTISERVICIOS S.A.C.}") String companyRazonSocial,
            @Value("${apisperu.company.direccion:AV. PUENTE PIEDRA 123}") String companyDireccion,
            @Value("${apisperu.company.ubigeo:150101}") String companyUbigeo) {
        this.companyDireccion = companyDireccion;
        this.companyUbigeo = companyUbigeo;
        this.apisPeruUrl = apisPeruUrl;
        this.apisPeruToken = apisPeruToken;
        this.apisPeruPdfUrl = pdfUrlConfigurada != null && !pdfUrlConfigurada.isBlank()
                ? pdfUrlConfigurada.trim()
                : apisPeruUrl.replaceAll("/send/?$", "/pdf");
        this.companyRuc = companyRuc;
        this.companyRazonSocial = companyRazonSocial;
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(java.time.Duration.ofSeconds(10));
        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(30));
        this.restClient = RestClient.builder().requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apisPeruToken.trim())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Emite una boleta de venta electrónica hacia APIsPERU y procesa la respuesta de SUNAT.
     */
    public BoletaResponse emitirBoleta(EmitirBoletaRequest request) {
        return enviarInvoice(construirInvoiceRequest(request));
    }

    /** Envia un invoice ya construido a APIsPERU y traduce la respuesta (sin persistir nada). */
    public BoletaResponse enviarInvoice(ApisPeruInvoiceRequest invoiceRequest) {
        String serie = invoiceRequest.getSerie();
        String correlativo = invoiceRequest.getCorrelativo();
        // APIsPERU rechaza (500) fechas con fracciones de segundo: se envian solo hasta los segundos
        if (invoiceRequest.getFechaEmision() != null) {
            invoiceRequest.setFechaEmision(invoiceRequest.getFechaEmision().replaceFirst("\\.\\d+(?=[+\\-Z])", ""));
        }

        try {
            log.info("Enviando boleta electrónica {}-{} a APIsPERU ({})", serie, correlativo, apisPeruUrl);

            java.util.concurrent.atomic.AtomicReference<String> falloServidor = new java.util.concurrent.atomic.AtomicReference<>();
            ApisPeruInvoiceResponse apisResponse = restClient.post()
                    .uri(apisPeruUrl)
                    .body(invoiceRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String rawError = new String(res.getBody().readAllBytes());
                        log.warn("Respuesta de error HTTP {} de APIsPERU: {}", res.getStatusCode(), rawError);
                        if (res.getStatusCode().is5xxServerError()) {
                            try {
                                log.warn("JSON enviado a APIsPERU: {}",
                                        tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(invoiceRequest));
                            } catch (Exception ignorado) {
                                // solo es diagnostico
                            }
                            falloServidor.set("HTTP " + res.getStatusCode().value() + ": " + rawError.replaceAll("\\s+", " ").trim());
                        }
                    })
                    .body(ApisPeruInvoiceResponse.class);

            if (falloServidor.get() != null) {
                return BoletaResponse.error(serie, correlativo, "APISPERU_SERVER_ERROR",
                        "APIsPERU o SUNAT (ambiente de pruebas) no pudo procesar la boleta; es un fallo del servicio, "
                                + "no de tus datos. Intenta de nuevo con Reenviar en unos minutos.",
                        List.of(falloServidor.get()), null);
            }

            if (apisResponse == null) {
                return BoletaResponse.error(serie, correlativo, "EMPTY_RESPONSE",
                        "No se recibió respuesta del servicio APIsPERU.",
                        List.of("Respuesta vacía"), null);
            }

            return mapearRespuesta(serie, correlativo, apisResponse);

        } catch (Exception ex) {
            log.error("Excepción al emitir boleta con APIsPERU", ex);
            return BoletaResponse.error(serie, correlativo, "COMMUNICATION_ERROR",
                    "Error al comunicarse con APIsPERU: " + ex.getMessage(),
                    List.of(ex.getMessage()), null);
        }
    }

    /** Pide a APIsPERU el PDF de un invoice (mismo JSON que /invoice/send). Devuelve los bytes del PDF. */
    public byte[] generarPdf(ApisPeruInvoiceRequest invoiceRequest) {
        try {
            byte[] pdf = restClient.post()
                    .uri(apisPeruPdfUrl)
                    .accept(MediaType.APPLICATION_PDF, MediaType.APPLICATION_OCTET_STREAM)
                    .body(invoiceRequest)
                    .retrieve()
                    .body(byte[].class);
            if (pdf == null || pdf.length < 5 || pdf[0] != '%' || pdf[1] != 'P' || pdf[2] != 'D' || pdf[3] != 'F') {
                throw new com.hcg.multiservicios.exception.BusinessException(HttpStatus.BAD_GATEWAY,
                        "PDF_INVALIDO", "APIsPERU no devolvió un PDF válido.");
            }
            return pdf;
        } catch (com.hcg.multiservicios.exception.BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error al generar PDF en APIsPERU", ex);
            throw new com.hcg.multiservicios.exception.BusinessException(HttpStatus.BAD_GATEWAY,
                    "PDF_NO_DISPONIBLE", "No se pudo generar el PDF en APIsPERU: " + ex.getMessage());
        }
    }

    /**
     * Envío directo de un objeto ApisPeruInvoiceRequest ya construido.
     */
    public ApisPeruInvoiceResponse enviarAapisPeru(ApisPeruInvoiceRequest invoiceRequest) {
        return restClient.post()
                .uri(apisPeruUrl)
                .body(invoiceRequest)
                .retrieve()
                .body(ApisPeruInvoiceResponse.class);
    }

    /**
     * Construye el JSON oficial de APIsPERU calculando bases imponibles, IGV al 18% y montos en letras.
     */
    public ApisPeruInvoiceRequest construirInvoiceRequest(EmitirBoletaRequest request) {
        ApisPeruInvoiceRequest invoice = new ApisPeruInvoiceRequest();
        boolean factura = "01".equals(request.getTipoDoc());
        invoice.setTipoDoc(factura ? "01" : "03"); // 01 Factura, 03 Boleta de Venta
        invoice.setSerie(request.getSerie() != null && !request.getSerie().isBlank() ? request.getSerie().trim()
                : factura ? "FFF1" : "BBB1");

        // Correlativo: si no viene, autogenerar número basado en timestamp
        if (request.getCorrelativo() != null && !request.getCorrelativo().isBlank()) {
            invoice.setCorrelativo(request.getCorrelativo().trim());
        } else {
            long autoNumero = (System.currentTimeMillis() / 1000) % 900000 + 100000;
            invoice.setCorrelativo(String.valueOf(autoNumero));
        }

        // Fecha de emisión
        if (request.getFechaEmision() != null && !request.getFechaEmision().isBlank()) {
            invoice.setFechaEmision(request.getFechaEmision());
        } else {
            invoice.setFechaEmision(OffsetDateTime.now(ZONA_PERU).truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }

        invoice.setTipoMoneda(request.getTipoMoneda() != null ? request.getTipoMoneda() : "PEN");
        invoice.setObservacion(request.getObservacion());

        // Empresa emisora (HCG Multiservicios)
        ApisPeruCompany empresa = new ApisPeruCompany(companyRuc, companyRazonSocial);
        empresa.setNombreComercial(companyRazonSocial);
        ApisPeruAddress dirEmpresa = new ApisPeruAddress(companyDireccion);
        dirEmpresa.setUbigeo(companyUbigeo);
        dirEmpresa.setDepartamento("LIMA");
        dirEmpresa.setProvincia("LIMA");
        dirEmpresa.setDistrito("LIMA");
        empresa.setAddress(dirEmpresa);
        invoice.setCompany(empresa);

        // Cliente receptor
        invoice.setClient(construirCliente(request.getCliente()));

        // Detalle de ítems y cálculo de impuestos
        List<ApisPeruDetailItem> details = new ArrayList<>();
        BigDecimal sumValorVenta = BigDecimal.ZERO;
        BigDecimal sumIgv = BigDecimal.ZERO;

        List<ItemBoletaDto> itemsInput = request.getItems() != null && !request.getItems().isEmpty()
                ? request.getItems()
                : List.of(new ItemBoletaDto("PRD-01", "Servicio General", BigDecimal.ONE, BigDecimal.valueOf(118.00)));

        int itemIndex = 1;
        for (ItemBoletaDto itemDto : itemsInput) {
            ApisPeruDetailItem detail = new ApisPeruDetailItem();
            detail.setCodProducto(itemDto.getCodProducto() != null ? itemDto.getCodProducto() : "ITEM-" + itemIndex);
            detail.setDescripcion(itemDto.getDescripcion() != null ? itemDto.getDescripcion() : "Item " + itemIndex);
            detail.setUnidad(itemDto.getUnidad() != null ? itemDto.getUnidad() : "NIU");
            detail.setTipAfeIgv("10"); // Gravado - Operación Onerosa
            detail.setPorcentajeIgv(BigDecimal.valueOf(18.0));

            BigDecimal cantidad = itemDto.getCantidad() != null && itemDto.getCantidad().compareTo(BigDecimal.ZERO) > 0
                    ? itemDto.getCantidad()
                    : BigDecimal.ONE;
            detail.setCantidad(cantidad);

            BigDecimal precioUnitario = itemDto.getPrecioUnitario();
            BigDecimal valorUnitario = itemDto.getValorUnitario();

            if (precioUnitario != null && precioUnitario.compareTo(BigDecimal.ZERO) >= 0) {
                // Se envió precio con IGV
                valorUnitario = precioUnitario.divide(IGV_FACTOR, 6, RoundingMode.HALF_UP);
            } else if (valorUnitario != null && valorUnitario.compareTo(BigDecimal.ZERO) >= 0) {
                // Se envió valor sin IGV
                precioUnitario = valorUnitario.multiply(IGV_FACTOR).setScale(2, RoundingMode.HALF_UP);
            } else {
                // Por defecto 100 sin IGV / 118 con IGV
                valorUnitario = BigDecimal.valueOf(100.00);
                precioUnitario = BigDecimal.valueOf(118.00);
            }

            BigDecimal mtoValorUnitarioFinal = valorUnitario.setScale(2, RoundingMode.HALF_UP);
            BigDecimal mtoPrecioUnitarioFinal = precioUnitario.setScale(2, RoundingMode.HALF_UP);
            BigDecimal mtoValorVenta = mtoValorUnitarioFinal.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            BigDecimal mtoBaseIgv = mtoValorVenta;
            BigDecimal igv = mtoBaseIgv.multiply(IGV_TASA).setScale(2, RoundingMode.HALF_UP);

            detail.setMtoValorUnitario(mtoValorUnitarioFinal);
            detail.setMtoPrecioUnitario(mtoPrecioUnitarioFinal);
            detail.setMtoValorVenta(mtoValorVenta);
            detail.setMtoBaseIgv(mtoBaseIgv);
            detail.setIgv(igv);
            detail.setTotalImpuestos(igv);

            details.add(detail);
            sumValorVenta = sumValorVenta.add(mtoValorVenta);
            sumIgv = sumIgv.add(igv);
            itemIndex++;
        }

        invoice.setDetails(details);

        // Totales del comprobante
        BigDecimal mtoOperGravadas = request.getMtoOperGravadas() != null ? request.getMtoOperGravadas() : sumValorVenta;
        BigDecimal mtoIGV = request.getMtoIGV() != null ? request.getMtoIGV() : sumIgv;
        BigDecimal mtoImpVenta = request.getMtoImpVenta() != null ? request.getMtoImpVenta() : mtoOperGravadas.add(mtoIGV);

        if (mtoOperGravadas.compareTo(sumValorVenta) != 0 || mtoIGV.compareTo(sumIgv) != 0
                || mtoImpVenta.compareTo(sumValorVenta.add(sumIgv)) != 0) {
            throw com.hcg.multiservicios.exception.BusinessException.badRequest("TOTALES_INCONSISTENTES",
                    "Los totales deben coincidir con los importes calculados de los ítems.");
        }
        if (mtoImpVenta.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw com.hcg.multiservicios.exception.BusinessException.badRequest("MONTO_FUERA_DE_RANGO",
                    "El total supera el límite NUMERIC(12,2).");
        }
        invoice.setMtoOperGravadas(mtoOperGravadas.setScale(2, RoundingMode.HALF_UP));
        invoice.setMtoIGV(mtoIGV.setScale(2, RoundingMode.HALF_UP));
        invoice.setTotalImpuestos(mtoIGV.setScale(2, RoundingMode.HALF_UP));
        invoice.setValorVenta(mtoOperGravadas.setScale(2, RoundingMode.HALF_UP));
        invoice.setSubTotal(mtoImpVenta.setScale(2, RoundingMode.HALF_UP));
        invoice.setMtoImpVenta(mtoImpVenta.setScale(2, RoundingMode.HALF_UP));

        // Leyenda SUNAT 1000: Monto en letras
        String montoEnLetras = NumeroALetras.convertir(invoice.getMtoImpVenta(), invoice.getTipoMoneda());
        invoice.setLegends(List.of(new ApisPeruLegend("1000", montoEnLetras)));

        return invoice;
    }

    private ApisPeruClient construirCliente(ClienteBoletaDto cDto) {
        if (cDto == null) {
            return new ApisPeruClient("1", "00000000", "CLIENTE VARIOS");
        }

        String numDoc = cDto.getNumDoc() != null ? cDto.getNumDoc().trim() : "00000000";
        String tipoDoc = cDto.getTipoDoc();

        if (tipoDoc == null || tipoDoc.isBlank()) {
            if (numDoc.length() == 8) {
                tipoDoc = "1"; // DNI
            } else if (numDoc.length() == 11) {
                tipoDoc = "6"; // RUC
            } else {
                tipoDoc = "0"; // Sin documento
            }
        }

        String nombre = cDto.getRznSocial() != null && !cDto.getRznSocial().isBlank()
                ? cDto.getRznSocial().trim()
                : "CLIENTE";

        ApisPeruClient client = new ApisPeruClient(tipoDoc, numDoc, nombre);
        client.setEmail(cDto.getEmail());
        client.setTelephone(cDto.getTelefono());
        return client;
    }

    private BoletaResponse mapearRespuesta(String serie, String correlativo, ApisPeruInvoiceResponse apisResponse) {
        String hash = apisResponse.getHash();
        String xml = apisResponse.getXml();
        ApisPeruSunatResponse sunat = apisResponse.getSunatResponse();

        String pdfUrl = String.format("/api/v1/boletas/%s-%s/pdf", serie, correlativo);

        if (sunat != null) {
            if (Boolean.TRUE.equals(sunat.getSuccess())) {
                String codigoSunat = sunat.getCdrResponse() != null ? sunat.getCdrResponse().getCode() : "0";
                String descSunat = sunat.getCdrResponse() != null
                        ? sunat.getCdrResponse().getDescription()
                        : "Comprobante aceptado por SUNAT";
                String cdrZip = sunat.getCdrZip();

                return BoletaResponse.exito(serie, correlativo, hash, codigoSunat, descSunat, pdfUrl, xml, cdrZip, sunat);
            } else {
                // SUNAT rechazó o reportó error
                String codigoError = sunat.getError() != null ? sunat.getError().getCode() : "SUNAT_REJECTED";
                String mensajeError = sunat.getError() != null ? sunat.getError().getMessage() : "Comprobante rechazado por SUNAT";
                return BoletaResponse.error(serie, correlativo, codigoError, mensajeError,
                        Collections.singletonList(mensajeError), sunat);
            }
        }

        // Si APIsPERU devolvió error a nivel de validación o mensaje
        if (apisResponse.getMessage() != null || apisResponse.getError() != null) {
            String msg = apisResponse.getMessage() != null ? apisResponse.getMessage() : String.valueOf(apisResponse.getError());
            return BoletaResponse.error(serie, correlativo, "VALIDATION_ERROR", msg, List.of(msg), null);
        }

        return BoletaResponse.exito(serie, correlativo, hash, "0", "Boleta emitida correctamente", pdfUrl, xml, null, null);
    }
}


