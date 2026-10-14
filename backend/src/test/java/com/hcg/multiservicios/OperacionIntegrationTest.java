package com.hcg.multiservicios;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hcg.multiservicios.entity.Rol;
import com.hcg.multiservicios.entity.Usuario;
import com.hcg.multiservicios.repository.RolRepository;
import com.hcg.multiservicios.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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

/** Operaciones: alta, filtros, linea de tiempo, estados y permisos. Cada test se revierte. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OperacionIntegrationTest {

    private static final String PASS = "Secreta123";

    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired PasswordEncoder encoder;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        crearUsuario("admin_op", Rol.ADMIN);
        crearUsuario("empleado_op", Rol.EMPLEADO);
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
        return "Bearer " + JsonPath.read(res, "$.token");
    }

    private String crearOperacion(String t, String tipo, String titulo) throws Exception {
        String res = mvc.perform(post("/api/operations").header("Authorization", t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"" + tipo + "\",\"titulo\":\"" + titulo + "\",\"contraparteNombre\":\"Cliente de prueba\",\"monto\":150.50}"))
                .andDo(r -> {
                    if (r.getResponse().getStatus() != 201) {
                        throw new AssertionError("Respuesta " + r.getResponse().getStatus() + ": "
                                + r.getResponse().getContentAsString());
                    }
                })
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.codigo").isNotEmpty())
                .andExpect(jsonPath("$.eventos.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        return String.valueOf(JsonPath.<Number>read(res, "$.id"));
    }

    @Test
    void ciclo_de_vida_de_una_operacion() throws Exception {
        String t = login("admin_op");
        String id = crearOperacion(t, "VENTA", "Venta de cemento");

        mvc.perform(post("/api/operations/" + id + "/events").header("Authorization", t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"COTIZACION\",\"titulo\":\"Cotizacion enviada\",\"referencia\":\"COT-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"))
                .andExpect(jsonPath("$.eventos.length()").value(2));

        mvc.perform(patch("/api/operations/" + id + "/status").header("Authorization", t)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"COMPLETADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.fechaCierre").isNotEmpty());

        // una operacion cerrada ya no se modifica
        mvc.perform(post("/api/operations/" + id + "/events").header("Authorization", t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"NOTA\",\"titulo\":\"tarde\"}"))
                .andExpect(status().is4xxClientError());
        mvc.perform(patch("/api/operations/" + id + "/status").header("Authorization", t)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void filtra_y_resume() throws Exception {
        String t = login("admin_op");
        crearOperacion(t, "COMPRA", "Compra de fierro");
        crearOperacion(t, "PEDIDO", "Pedido de pintura");

        mvc.perform(get("/api/operations?tipo=COMPRA").header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tipo").value("COMPRA"));
        mvc.perform(get("/api/operations?q=pintura").header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/operations/summary").header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.abiertas").value(2))
                .andExpect(jsonPath("$.compras").value(1))
                .andExpect(jsonPath("$.pedidos").value(1));
    }

    @Test
    void rechaza_tipo_invalido() throws Exception {
        mvc.perform(post("/api/operations").header("Authorization", login("admin_op"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"OTRO\",\"titulo\":\"x\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void empleadoSinPermisosRecibe403() throws Exception {
        mvc.perform(get("/api/operations").header("Authorization", login("empleado_op")))
                .andExpect(status().isForbidden());
    }
}
