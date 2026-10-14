package com.hcg.multiservicios;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Eliminar clientes, carga masiva de productos y emisión de facturas (además de boletas). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MejorasIntegrationTest {

    private static final String PASS = "Secreta123";

    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired PasswordEncoder encoder;
    @MockitoSpyBean ApisPeruBoletaService apisPeru;

    MockMvc mvc;
    String token;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        Usuario u = new Usuario();
        u.setUsername("admin3");
        u.setCorreo("admin3@hcg.pe");
        u.setNombreCompleto("admin3");
        u.setPasswordHash(encoder.encode(PASS));
        u.setRoles(new HashSet<>(List.of(roles.findByNombre(Rol.ADMIN).orElseThrow())));
        usuarios.saveAndFlush(u);
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"admin3\",\"password\":\"" + PASS + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        token = JsonPath.read(res, "$.token");
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String json) {
        MockHttpServletRequestBuilder r = b.header("Authorization", "Bearer " + token);
        return json == null ? r : r.contentType(MediaType.APPLICATION_JSON).content(json);
    }

    // ---------- Clientes: eliminar ----------

    @Test
    @DisplayName("Un cliente sin movimientos se elimina y deja de existir")
    void eliminarCliente() throws Exception {
        String res = mvc.perform(auth(post("/api/clients"), "{\"tipoDocumento\":\"DNI\",\"dni\":\"45879632\",\"nombres\":\"Ana\",\"apellidos\":\"Ruiz\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(res, "$.id");

        mvc.perform(auth(delete("/api/clients/" + id), null)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/clients/" + id), null)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Eliminar un cliente inexistente responde 404")
    void eliminarClienteInexistente() throws Exception {
        mvc.perform(auth(delete("/api/clients/99999"), null)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Eliminar cliente sin token responde 401")
    void eliminarClienteSinToken() throws Exception {
        mvc.perform(delete("/api/clients/1")).andExpect(status().isUnauthorized());
    }

    // ---------- Productos: carga masiva ----------

    private String filas(boolean actualizar, String... filasJson) {
        return "{\"actualizarExistentes\":" + actualizar + ",\"filas\":[" + String.join(",", filasJson) + "]}";
    }

    private String fila(int n, String codigo, String nombre, String precio) {
        return "{\"fila\":" + n + ",\"producto\":{\"codigo\":" + (codigo == null ? "null" : "\"" + codigo + "\"")
                + ",\"nombre\":" + (nombre == null ? "null" : "\"" + nombre + "\"") + ",\"precioVenta\":" + precio + ",\"stock\":5}}";
    }

    @Test
    @DisplayName("Importar: crea las filas válidas e informa las inválidas y repetidas por número de fila")
    void importarProductos() throws Exception {
        String body = filas(false,
                fila(2, "IMP-1", "Cable 2.5mm", "10.50"),
                fila(3, null, null, "5"),
                fila(4, "IMP-1", "Repetido en archivo", "7"),
                fila(5, null, "Sin código", "3"));
        mvc.perform(auth(post("/api/products/import"), body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.creados").value(2))
                .andExpect(jsonPath("$.actualizados").value(0))
                .andExpect(jsonPath("$.errores", hasSize(2)))
                .andExpect(jsonPath("$.errores[0].fila").value(3))
                .andExpect(jsonPath("$.errores[1].fila").value(4));

        mvc.perform(auth(get("/api/products?q=IMP-1"), null))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].stock").value(5));
    }

    @Test
    @DisplayName("Importar: un código existente se omite, o se actualiza si se pide")
    void importarCodigoExistente() throws Exception {
        mvc.perform(auth(post("/api/products/import"), filas(false, fila(2, "IMP-9", "Original", "10")))).andExpect(status().isOk());

        mvc.perform(auth(post("/api/products/import"), filas(false, fila(2, "IMP-9", "Nuevo nombre", "20"))))
                .andExpect(jsonPath("$.creados").value(0))
                .andExpect(jsonPath("$.errores", hasSize(1)));

        mvc.perform(auth(post("/api/products/import"), filas(true, fila(2, "IMP-9", "Nuevo nombre", "20"))))
                .andExpect(jsonPath("$.actualizados").value(1))
                .andExpect(jsonPath("$.errores", hasSize(0)));

        mvc.perform(auth(get("/api/products?q=IMP-9"), null))
                .andExpect(jsonPath("$.content[0].nombre").value("Nuevo nombre"))
                .andExpect(jsonPath("$.content[0].precioVenta").value(20.0));
    }

    @Test
    @DisplayName("Importar sin filas responde 400 y sin token 401")
    void importarValidaciones() throws Exception {
        mvc.perform(auth(post("/api/products/import"), "{\"filas\":[]}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/products/import").contentType(MediaType.APPLICATION_JSON).content("{\"filas\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Comprobantes: factura ----------

    private static final String ITEMS = "\"items\":[{\"descripcion\":\"Servicio\",\"cantidad\":1,\"precioUnitario\":118.00}]";

    @Test
    @DisplayName("La factura exige cliente con RUC de 11 dígitos")
    void facturaRequiereRuc() throws Exception {
        String conDni = "{\"tipoDoc\":\"01\",\"cliente\":{\"tipoDoc\":\"1\",\"numDoc\":\"45879632\",\"rznSocial\":\"JUAN\"}," + ITEMS + "}";
        mvc.perform(auth(post("/api/v1/boletas/emitir"), conDni))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("FACTURA_REQUIERE_RUC"));

        String rucCorto = "{\"tipoDoc\":\"01\",\"cliente\":{\"tipoDoc\":\"6\",\"numDoc\":\"2010\",\"rznSocial\":\"EMPRESA\"}," + ITEMS + "}";
        mvc.perform(auth(post("/api/v1/boletas/emitir"), rucCorto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("FACTURA_REQUIERE_RUC"));
    }

    @Test
    @DisplayName("Tipo de comprobante desconocido responde 400")
    void tipoInvalido() throws Exception {
        String json = "{\"tipoDoc\":\"07\",\"cliente\":{\"tipoDoc\":\"1\",\"numDoc\":\"45879632\",\"rznSocial\":\"JUAN\"}," + ITEMS + "}";
        mvc.perform(auth(post("/api/v1/boletas/emitir"), json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TIPO_COMPROBANTE_INVALIDO"));
    }

    @Test
    @DisplayName("Una factura a RUC se emite con serie FFF1 y se puede filtrar por tipo")
    void emitirFactura() throws Exception {
        doReturn(BoletaResponse.exito("FFF1", "1", "hash", "0", "Aceptada", null, "<xml/>", "cdr", null))
                .when(apisPeru).enviarInvoice(any());
        String factura = "{\"tipoDoc\":\"01\",\"cliente\":{\"tipoDoc\":\"6\",\"numDoc\":\"20100070970\",\"rznSocial\":\"EMPRESA SAC\"}," + ITEMS + "}";
        mvc.perform(auth(post("/api/v1/boletas/emitir"), factura))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serie").value("FFF1"));

        String boleta = "{\"cliente\":{\"tipoDoc\":\"1\",\"numDoc\":\"45879632\",\"rznSocial\":\"JUAN\"}," + ITEMS + "}";
        mvc.perform(auth(post("/api/v1/boletas/emitir"), boleta)).andExpect(status().isOk());

        mvc.perform(auth(get("/api/v1/boletas?tipo=01"), null))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].tipoDoc").value("01"))
                .andExpect(jsonPath("$.content[0].serie").value("FFF1"));
        mvc.perform(auth(get("/api/v1/boletas?tipo=03"), null))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].serie").value("BBB1"));
        mvc.perform(auth(get("/api/documents?tipo=FACTURA"), null))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
