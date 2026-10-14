package com.hcg.multiservicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hcg.multiservicios.dto.apisperu.ApisPeruInvoiceRequest;
import com.hcg.multiservicios.dto.apisperu.ClienteBoletaDto;
import com.hcg.multiservicios.dto.apisperu.EmitirBoletaRequest;
import com.hcg.multiservicios.dto.apisperu.ItemBoletaDto;
import com.hcg.multiservicios.service.ApisPeruBoletaService;
import com.hcg.multiservicios.util.NumeroALetras;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class BoletaIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ApisPeruBoletaService boletaService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("NumeroALetras convierte correctamente montos a texto SUNAT")
    void testNumeroALetras() {
        String texto118 = NumeroALetras.convertir(BigDecimal.valueOf(118.00), "PEN");
        assertThat(texto118).isEqualTo("SON CIENTO DIECIOCHO CON 00/100 SOLES");

        String texto10050 = NumeroALetras.convertir(BigDecimal.valueOf(100.50), "PEN");
        assertThat(texto10050).isEqualTo("SON CIEN CON 50/100 SOLES");
    }

    @Test
    @DisplayName("Construcción de JSON para APIsPERU calcula IGV 18%, totales y empresa emisora")
    void testConstruccionInvoiceRequest() {
        EmitirBoletaRequest request = new EmitirBoletaRequest();
        request.setSerie("BBB1");
        request.setCorrelativo("1");
        request.setCliente(new ClienteBoletaDto("1", "45879632", "JUAN PEREZ"));
        request.setItems(List.of(
                new ItemBoletaDto("P001", "CAMBIO DE ACEITE", BigDecimal.ONE, BigDecimal.valueOf(118.00))
        ));

        ApisPeruInvoiceRequest invoice = boletaService.construirInvoiceRequest(request);

        assertThat(invoice.getTipoDoc()).isEqualTo("03");
        assertThat(invoice.getSerie()).isEqualTo("BBB1");
        assertThat(invoice.getCorrelativo()).isEqualTo("1");
        assertThat(invoice.getCompany().getRuc()).isEqualTo("20026788123");
        assertThat(invoice.getCompany().getRazonSocial()).isEqualTo("HCG MULTISERVICIOS S.A.C.");
        assertThat(invoice.getClient().getNumDoc()).isEqualTo("45879632");
        assertThat(invoice.getClient().getTipoDoc()).isEqualTo("1");
        assertThat(invoice.getMtoOperGravadas()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(invoice.getMtoIGV()).isEqualByComparingTo(BigDecimal.valueOf(18.00));
        assertThat(invoice.getMtoImpVenta()).isEqualByComparingTo(BigDecimal.valueOf(118.00));
        assertThat(invoice.getLegends()).isNotEmpty();
        assertThat(invoice.getLegends().get(0).getValue()).contains("CIENTO DIECIOCHO");
    }

    @Test
    @DisplayName("Emitir boleta ya no es público: sin token responde 401")
    void testEmitirRequiereAutenticacion() throws Exception {
        String jsonBody = "{\"cliente\":{\"tipoDoc\":\"1\",\"numDoc\":\"12345678\",\"rznSocial\":\"CLIENTE TEST\"},\"items\":[{\"descripcion\":\"Item\",\"cantidad\":1,\"precioUnitario\":59.00}]}";
        mockMvc.perform(post("/api/v1/boletas/emitir").contentType(MediaType.APPLICATION_JSON).content(jsonBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Consultas de boletas sin token responden 401")
    void testConsultasRequierenAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/boletas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/boletas/BBB1-1/pdf")).andExpect(status().isUnauthorized());
    }
}
