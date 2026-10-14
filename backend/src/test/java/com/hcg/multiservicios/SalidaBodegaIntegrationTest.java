package com.hcg.multiservicios;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hcg.multiservicios.entity.Proveedor;
import com.hcg.multiservicios.entity.Rol;
import com.hcg.multiservicios.entity.Usuario;
import com.hcg.multiservicios.repository.ProveedorRepository;
import com.hcg.multiservicios.repository.RolRepository;
import com.hcg.multiservicios.repository.UsuarioRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Salidas de bodega: descuento y devolución de stock, validaciones y permisos. Cada test se revierte. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SalidaBodegaIntegrationTest {

    private static final String PASS = "Secreta123";

    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired ProveedorRepository proveedores;
    @Autowired PasswordEncoder encoder;

    MockMvc mvc;
    String admin;
    int productoId;
    int proveedorId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        crearUsuario("admin_bod", Rol.ADMIN);
        crearUsuario("empleado_bod", Rol.EMPLEADO);
        admin = "Bearer " + login("admin_bod");

        String res = mvc.perform(post("/api/products").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"BOD-001\",\"nombre\":\"Arroz 5kg\",\"precioVenta\":25.00,\"stock\":10,\"stockMinimo\":1,\"unidadMedida\":\"NIU\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        productoId = JsonPath.read(res, "$.id");

        Proveedor p = new Proveedor();
        p.setRuc("20100070970");
        p.setRazonSocial("PROVEEDOR BODEGA SAC");
        p.setNombre("PROVEEDOR BODEGA SAC");
        proveedorId = proveedores.saveAndFlush(p).getId();
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

    private String salida(int cantidad) {
        return "{\"destinoTipo\":\"PROVEEDOR\",\"proveedorId\":" + proveedorId + ",\"motivo\":\"ENVIO\","
                + "\"items\":[{\"codigo\":\"BOD-001\",\"cantidad\":" + cantidad + "}]}";
    }

    private void stockEs(int esperado) throws Exception {
        mvc.perform(get("/api/products/" + productoId).header("Authorization", admin))
                .andExpect(jsonPath("$.stock").value(esperado));
    }

    @Test
    @DisplayName("Registrar salida por código descuenta stock, guarda stock anterior/posterior y crea el documento")
    void registrarDescuentaStock() throws Exception {
        mvc.perform(post("/api/warehouse-exits").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(salida(4)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value(org.hamcrest.Matchers.startsWith("SAL-")))
                .andExpect(jsonPath("$.estado").value("REGISTRADA"))
                .andExpect(jsonPath("$.destinoNombre").value("PROVEEDOR BODEGA SAC"))
                .andExpect(jsonPath("$.items[0].stockAnterior").value(10))
                .andExpect(jsonPath("$.items[0].stockPosterior").value(6));
        stockEs(6);

        mvc.perform(get("/api/documents?tipo=SALIDA_BODEGA").header("Authorization", admin))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/warehouse-exits").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Stock insuficiente da 400 y no descuenta nada")
    void stockInsuficiente() throws Exception {
        mvc.perform(post("/api/warehouse-exits").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(salida(11)))
                .andExpect(status().isBadRequest());
        stockEs(10);
    }

    @Test
    @DisplayName("Anular devuelve el stock y no se puede anular dos veces")
    void anularDevuelveStock() throws Exception {
        String res = mvc.perform(post("/api/warehouse-exits").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(salida(4)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(res, "$.id");
        stockEs(6);

        mvc.perform(post("/api/warehouse-exits/" + id + "/anular").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"));
        stockEs(10);
        mvc.perform(post("/api/warehouse-exits/" + id + "/anular").header("Authorization", admin))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/documents?tipo=SALIDA_BODEGA").header("Authorization", admin))
                .andExpect(jsonPath("$.content[0].estado").value("ANULADA"));
    }

    @Test
    @DisplayName("Validaciones: destino sin proveedor, producto inexistente; permisos 401/403")
    void validacionesYPermisos() throws Exception {
        mvc.perform(post("/api/warehouse-exits").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinoTipo\":\"PROVEEDOR\",\"motivo\":\"ENVIO\",\"items\":[{\"codigo\":\"BOD-001\",\"cantidad\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.proveedorId").exists());
        mvc.perform(post("/api/warehouse-exits").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinoTipo\":\"OTRO\",\"destinoNombre\":\"Local 2\",\"motivo\":\"MERMA\",\"items\":[{\"codigo\":\"NO-EXISTE\",\"cantidad\":1}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/warehouse-exits").header("Authorization", "Bearer " + login("empleado_bod"))
                        .contentType(MediaType.APPLICATION_JSON).content(salida(1)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/warehouse-exits")).andExpect(status().isUnauthorized());
        stockEs(10);
    }
}
