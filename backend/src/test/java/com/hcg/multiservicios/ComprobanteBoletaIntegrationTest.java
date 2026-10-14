package com.hcg.multiservicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hcg.multiservicios.dto.apisperu.BoletaResponse;
import com.hcg.multiservicios.entity.Comprobante;
import com.hcg.multiservicios.entity.Rol;
import com.hcg.multiservicios.entity.Usuario;
import com.hcg.multiservicios.repository.ComprobanteRepository;
import com.hcg.multiservicios.repository.RolRepository;
import com.hcg.multiservicios.repository.UsuarioRepository;
import com.hcg.multiservicios.service.ApisPeruBoletaService;
import com.jayway.jsonpath.JsonPath;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Persistencia de boletas con APIsPERU simulado (no se llama a SUNAT). Cada test se revierte. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ComprobanteBoletaIntegrationTest {

    private static final String PASS = "Secreta123";
    private static final String BODY = "{\"cliente\":{\"tipoDoc\":\"1\",\"numDoc\":\"12345678\",\"rznSocial\":\"CLIENTE TEST\"},"
            + "\"items\":[{\"codProducto\":\"P01\",\"descripcion\":\"Item Test\",\"cantidad\":2,\"precioUnitario\":59.00}]}";

    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired PasswordEncoder encoder;
    @Autowired ComprobanteRepository comprobantes;
    @MockitoSpyBean ApisPeruBoletaService apisPeru;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        crearUsuario("admin_bol", Rol.ADMIN);
        crearUsuario("empleado_bol", Rol.EMPLEADO);
    }

    private void crearUsuario(String username, String rol) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setCorreo(username + "@hcg.pe");
        u.setNombreCompleto(username);
        u.setPasswordHash(encoder.encode(PASS));
        u.setRoles(new HashSet<>(List.of(roles.findByNombre(rol).orElseThrow())));
        usuarios.saveAndFlush(u);
    }

    private String login(String username) throws Exception {
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"" + username + "\",\"password\":\"" + PASS + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(res, "$.token");
    }

    @Test
    void boletaSinItemsONumerosInvalidosDa400SinPersistir() throws Exception {
        String token = "Bearer " + login("admin_bol");
        for (String body : List.of("{}", BODY.replace("2,", "-1,"),
                BODY.replace("59.00", "-59.00"), BODY.replace("59.00", "59.001"))) {
            mvc.perform(post("/api/v1/boletas/emitir").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
        assertThat(comprobantes.count()).isZero();
    }

    @Test
    void totalesManipuladosNoSeAceptan() throws Exception {
        mvc.perform(post("/api/v1/boletas/emitir").header("Authorization", "Bearer " + login("admin_bol"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("{\"cliente\"", "{\"mtoImpVenta\":1,\"cliente\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TOTALES_INCONSISTENTES"));
        assertThat(comprobantes.count()).isZero();
    }

    private void apisPeruAcepta() {
        doReturn(BoletaResponse.exito("BBB1", "1", "hash123", "0", "La Boleta ha sido aceptada", null, "<xml/>", "cdr", null))
                .when(apisPeru).enviarInvoice(any());
    }

    @Test
    @DisplayName("PDF: boleta aceptada devuelve application/pdf; inexistente da 404")
    void pdfDeBoletaAceptada() throws Exception {
        String token = "Bearer " + login("admin_bol");
        apisPeruAcepta();
        String res = mvc.perform(post("/api/v1/boletas/emitir").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andReturn().getResponse().getContentAsString();
        String serie = JsonPath.read(res, "$.serie");
        String corr = JsonPath.read(res, "$.correlativo");
        doReturn("%PDF-1.4 fake".getBytes()).when(apisPeru).generarPdf(any());
        mvc.perform(get("/api/v1/boletas/" + serie + "-" + corr + "/pdf").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType(MediaType.APPLICATION_PDF));
        mvc.perform(get("/api/v1/boletas/" + serie + "-999999/pdf").header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Empleado sin permiso COMPROBANTES_CREAR recibe 403")
    void sinPermisoDa403() throws Exception {
        mvc.perform(post("/api/v1/boletas/emitir").header("Authorization", "Bearer " + login("empleado_bol"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Emitir guarda la boleta ACEPTADA con correlativo secuencial y detalle")
    void emitirGuardaYNumeraSecuencialmente() throws Exception {
        apisPeruAcepta();
        String token = "Bearer " + login("admin_bol");

        String r1 = mvc.perform(post("/api/v1/boletas/emitir").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACEPTADA"))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/v1/boletas/emitir").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());

        assertThat(comprobantes.count()).isEqualTo(2);
        int c1 = Integer.parseInt(JsonPath.read(r1, "$.correlativo"));
        Comprobante guardado = comprobantes.findByTipoDocAndSerieAndCorrelativo("03", "BBB1", c1).orElseThrow();
        assertThat(guardado.getEstado()).isEqualTo(Comprobante.ACEPTADA);
        assertThat(guardado.getHash()).isEqualTo("hash123");
        assertThat(guardado.getXml()).isEqualTo("<xml/>");
        assertThat(guardado.getMtoImpVenta()).isEqualByComparingTo("118.00");
        assertThat(guardado.getUsuarioId()).isNotNull();
        assertThat(comprobantes.findByTipoDocAndSerieAndCorrelativo("03", "BBB1", c1 + 1)).isPresent();

        mvc.perform(get("/api/v1/boletas/BBB1-" + c1).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles[0].descripcion").value("Item Test"))
                .andExpect(jsonPath("$.mtoImpVenta").value(118.00));
        mvc.perform(get("/api/v1/boletas?estado=ACEPTADA").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Si APIsPERU no responde queda en ERROR y se puede reenviar con el mismo número")
    void errorDeComunicacionSeReenvia() throws Exception {
        doReturn(BoletaResponse.error("BBB1", "1", "COMMUNICATION_ERROR", "sin conexión", List.of("sin conexión"), null))
                .when(apisPeru).enviarInvoice(any());
        String token = "Bearer " + login("admin_bol");

        String r = mvc.perform(post("/api/v1/boletas/emitir").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadGateway())
                .andReturn().getResponse().getContentAsString();
        String correlativo = JsonPath.read(r, "$.correlativo");
        assertThat(comprobantes.count()).isEqualTo(1);
        assertThat(comprobantes.findAll().get(0).getEstado()).isEqualTo(Comprobante.ERROR);

        apisPeruAcepta();
        mvc.perform(post("/api/v1/boletas/BBB1-" + correlativo + "/reenviar").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACEPTADA"));
        assertThat(comprobantes.count()).isEqualTo(1);
        assertThat(comprobantes.findAll().get(0).getEstado()).isEqualTo(Comprobante.ACEPTADA);

        // Una boleta ya aceptada no se puede reenviar
        mvc.perform(post("/api/v1/boletas/BBB1-" + correlativo + "/reenviar").header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Rechazo de SUNAT queda RECHAZADA (no reenviable)")
    void rechazoQuedaRechazada() throws Exception {
        doReturn(BoletaResponse.error("BBB1", "1", "2335", "Documento inválido", List.of("Documento inválido"), null))
                .when(apisPeru).enviarInvoice(any());
        mvc.perform(post("/api/v1/boletas/emitir").header("Authorization", "Bearer " + login("admin_bol"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest());
        assertThat(comprobantes.findAll().get(0).getEstado()).isEqualTo(Comprobante.RECHAZADA);
    }
}

