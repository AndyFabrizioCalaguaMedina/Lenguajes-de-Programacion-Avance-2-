package com.hcg.multiservicios;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hcg.multiservicios.dto.apisperu.BoletaResponse;
import com.hcg.multiservicios.entity.Rol;
import com.hcg.multiservicios.entity.Usuario;
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

/** Cotizaciones y registro de documentos, con APIsPERU simulado. Cada test se revierte. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CotizacionDocumentoIntegrationTest {

    private static final String PASS = "Secreta123";
    private static final String COTIZACION = "{\"clienteNombre\":\"JUAN PEREZ\",\"items\":["
            + "{\"descripcion\":\"Mantenimiento\",\"cantidad\":2,\"precioUnitario\":59.00}]}";

    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired PasswordEncoder encoder;
    @MockitoSpyBean ApisPeruBoletaService apisPeru;

    MockMvc mvc;
    String admin;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        crearUsuario("admin_cot", Rol.ADMIN);
        crearUsuario("empleado_cot", Rol.EMPLEADO);
        admin = "Bearer " + login("admin_cot");
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

    private int crearCotizacion() throws Exception {
        String res = mvc.perform(post("/api/quotations").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(COTIZACION))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(res, "$.id");
    }

    private void estado(int id, String estado, int esperado) throws Exception {
        mvc.perform(patch("/api/quotations/" + id + "/status").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"" + estado + "\"}"))
                .andExpect(status().is(esperado));
    }

    @Test
    @DisplayName("Crear cotización calcula IGV, numera y se registra en documentos")
    void crearCalculaYRegistraDocumento() throws Exception {
        mvc.perform(post("/api/quotations").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(COTIZACION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value(org.hamcrest.Matchers.startsWith("COT-")))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.total").value(118.00))
                .andExpect(jsonPath("$.subtotal").value(100.00))
                .andExpect(jsonPath("$.igv").value(18.00))
                .andExpect(jsonPath("$.items[0].subtotal").value(118.00));

        mvc.perform(get("/api/documents?tipo=COTIZACION").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].origen").value("COTIZACION"));
    }

    @Test
    @DisplayName("Validaciones: sin ítems o sin cliente da 400; sin permiso da 403")
    void validacionesYPermisos() throws Exception {
        mvc.perform(post("/api/quotations").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"clienteNombre\":\"X\",\"items\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/quotations").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"descripcion\":\"A\",\"cantidad\":1,\"precioUnitario\":10}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.clienteNombre").exists());
        mvc.perform(post("/api/quotations").header("Authorization", "Bearer " + login("empleado_cot"))
                        .contentType(MediaType.APPLICATION_JSON).content(COTIZACION))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/quotations")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Transiciones de estado válidas e inválidas")
    void transicionesDeEstado() throws Exception {
        int id = crearCotizacion();
        estado(id, "APROBADA", 409); // BORRADOR no puede pasar directo a APROBADA
        estado(id, "ENVIADA", 200);
        estado(id, "APROBADA", 200);
        mvc.perform(get("/api/documents?tipo=COTIZACION").header("Authorization", admin))
                .andExpect(jsonPath("$.content[0].estado").value("APROBADA"));
    }

    @Test
    @DisplayName("Convertir cotización aprobada en boleta: queda FACTURADA y se registra la boleta")
    void convertirEnBoleta() throws Exception {
        doReturn(BoletaResponse.exito("BBB1", "1", "hash", "0", "Aceptada", null, "<xml/>", "cdr", null))
                .when(apisPeru).enviarInvoice(any());
        int id = crearCotizacion();

        // sin estar aprobada no se puede facturar
        mvc.perform(post("/api/quotations/" + id + "/convertir-boleta").header("Authorization", admin))
                .andExpect(status().isConflict());

        estado(id, "ENVIADA", 200);
        estado(id, "APROBADA", 200);
        mvc.perform(post("/api/quotations/" + id + "/convertir-boleta").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACEPTADA"));

        mvc.perform(get("/api/quotations/" + id).header("Authorization", admin))
                .andExpect(jsonPath("$.estado").value("FACTURADA"))
                .andExpect(jsonPath("$.comprobanteId").isNumber());
        mvc.perform(get("/api/documents?tipo=BOLETA").header("Authorization", admin))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].estado").value("ACEPTADA"));

        // no se factura dos veces
        mvc.perform(post("/api/quotations/" + id + "/convertir-boleta").header("Authorization", admin))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Si APIsPERU falla, la cotización sigue APROBADA y no se duplica la boleta")
    void conversionFallidaNoFactura() throws Exception {
        doReturn(BoletaResponse.error("BBB1", "1", "COMMUNICATION_ERROR", "sin conexión", List.of("x"), null))
                .when(apisPeru).enviarInvoice(any());
        int id = crearCotizacion();
        estado(id, "ENVIADA", 200);
        estado(id, "APROBADA", 200);

        mvc.perform(post("/api/quotations/" + id + "/convertir-boleta").header("Authorization", admin))
                .andExpect(status().isBadGateway());
        mvc.perform(get("/api/quotations/" + id).header("Authorization", admin))
                .andExpect(jsonPath("$.estado").value("APROBADA"));
        mvc.perform(post("/api/quotations/" + id + "/convertir-boleta").header("Authorization", admin))
                .andExpect(status().isConflict()); // debe reenviarse la boleta existente
    }

    @Test
    @DisplayName("Documento manual: crear, duplicado 409, anular; los automáticos no se anulan")
    void documentoManual() throws Exception {
        String body = "{\"tipo\":\"FACTURA\",\"numero\":\"F001-100\",\"fecha\":\"2026-10-08\","
                + "\"contraparteNombre\":\"PROVEEDOR SAC\",\"monto\":250.50}";
        String res = mvc.perform(post("/api/documents").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origen").value("MANUAL"))
                .andReturn().getResponse().getContentAsString();
        int docId = JsonPath.read(res, "$.id");

        mvc.perform(post("/api/documents").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/documents/" + docId + "/anular").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADO"));

        crearCotizacion();
        String lista = mvc.perform(get("/api/documents?tipo=COTIZACION").header("Authorization", admin))
                .andReturn().getResponse().getContentAsString();
        int autoId = JsonPath.read(lista, "$.content[0].id");
        mvc.perform(post("/api/documents/" + autoId + "/anular").header("Authorization", admin))
                .andExpect(status().isConflict());
    }
    @Test
    void putCotizacionActualizaElRegistroCentralSinDuplicarlo() throws Exception {
        int id = crearCotizacion();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/quotations/" + id)
                        .header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(COTIZACION.replace("59.00", "118.00")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(236.00));
        mvc.perform(get("/api/documents?tipo=COTIZACION").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].monto").value(236.00))
                .andExpect(jsonPath("$.content[0].origenId").value(String.valueOf(id)))
                .andExpect(jsonPath("$.content[0].creadoEn").exists());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/quotations/" + id)
                        .header("Authorization", admin))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void putDocumentoManualActualizaDatosYRegistraEvento() throws Exception {
        String body = "{\"tipo\":\"OTRO\",\"numero\":\"AUDIT-PUT\",\"fecha\":\"2026-10-08\",\"monto\":10.50}";
        String res = mvc.perform(post("/api/documents").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(res, "$.id");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/documents/" + id)
                        .header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("10.50", "20.75")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.monto").value(20.75));
        mvc.perform(get("/api/audit").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].accion").value("EDITAR_DOCUMENTO"));
    }
}

