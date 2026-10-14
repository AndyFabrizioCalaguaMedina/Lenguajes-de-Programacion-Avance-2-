package com.hcg.multiservicios.dto.apisperu;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BoletaResponse {
    private Long id;
    private boolean success;
    private String estado; // "ACEPTADA", "RECHAZADA", "ERROR"
    private String serie;
    private String correlativo;
    private String codigoRespuestaSunat;
    private String descripcionSunat;
    private String hash;
    private String pdfUrl;
    private String xml;
    private String cdrZip;
    private List<String> errores;
    private ApisPeruSunatResponse sunatResponse;

    public BoletaResponse() {}

    public static BoletaResponse exito(String serie, String correlativo, String hash, String codigoSunat, String descripcionSunat, String pdfUrl, String xml, String cdrZip, ApisPeruSunatResponse sunatResponse) {
        BoletaResponse res = new BoletaResponse();
        res.setSuccess(true);
        res.setEstado("ACEPTADA");
        res.setSerie(serie);
        res.setCorrelativo(correlativo);
        res.setHash(hash);
        res.setCodigoRespuestaSunat(codigoSunat);
        res.setDescripcionSunat(descripcionSunat);
        res.setPdfUrl(pdfUrl);
        res.setXml(xml);
        res.setCdrZip(cdrZip);
        res.setSunatResponse(sunatResponse);
        return res;
    }

    public static BoletaResponse error(String serie, String correlativo, String codigoError, String mensajeError, List<String> errores, ApisPeruSunatResponse sunatResponse) {
        BoletaResponse res = new BoletaResponse();
        res.setSuccess(false);
        res.setEstado("ERROR");
        res.setSerie(serie);
        res.setCorrelativo(correlativo);
        res.setCodigoRespuestaSunat(codigoError);
        res.setDescripcionSunat(mensajeError);
        res.setErrores(errores);
        res.setSunatResponse(sunatResponse);
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }

    public String getCorrelativo() { return correlativo; }
    public void setCorrelativo(String correlativo) { this.correlativo = correlativo; }

    public String getCodigoRespuestaSunat() { return codigoRespuestaSunat; }
    public void setCodigoRespuestaSunat(String codigoRespuestaSunat) { this.codigoRespuestaSunat = codigoRespuestaSunat; }

    public String getDescripcionSunat() { return descripcionSunat; }
    public void setDescripcionSunat(String descripcionSunat) { this.descripcionSunat = descripcionSunat; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public String getXml() { return xml; }
    public void setXml(String xml) { this.xml = xml; }

    public String getCdrZip() { return cdrZip; }
    public void setCdrZip(String cdrZip) { this.cdrZip = cdrZip; }

    public List<String> getErrores() { return errores; }
    public void setErrores(List<String> errores) { this.errores = errores; }

    public ApisPeruSunatResponse getSunatResponse() { return sunatResponse; }
    public void setSunatResponse(ApisPeruSunatResponse sunatResponse) { this.sunatResponse = sunatResponse; }
}
