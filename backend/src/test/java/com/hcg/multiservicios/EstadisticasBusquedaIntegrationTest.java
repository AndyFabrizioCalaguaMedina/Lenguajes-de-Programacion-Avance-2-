package com.hcg.multiservicios;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/** Estadisticas, historial filtrable y busqueda global. Cada test se revierte. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EstadisticasBusquedaIntegrationTest {

    private static final String PASS = "Secreta123";

    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired PasswordEncoder encoder;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        crearUsuario("admin_est", Rol.ADMIN);
        crearUsuario("empleado_est", Rol.EMPLEADO);
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

    @Test
    void adminVeDashboardYReportes() throws Exception {
        String t = login("admin_est");
        mvc.perform(get("/api/stats/dashboard").header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventasPorDia.length()").value(14))
                .andExpect(jsonPath("$.cotizacionesPorEstado.length()").value(5));
        mvc.perform(get("/api/stats/reportes").header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventasPorDia.length()").value(30))
                .andExpect(jsonPath("$.actividadPorUsuario").isArray());
    }

    @Test
    void reportesRechazaRangoInvalido() throws Exception {
        mvc.perform(get("/api/stats/reportes?desde=2026-10-10&hasta=2026-10-01")
                        .header("Authorization", login("admin_est")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("RANGO_INVALIDO"));
    }

    @Test
    void empleadoSinPermisosRecibe403() throws Exception {
        String t = login("empleado_est");
        mvc.perform(get("/api/stats/dashboard").header("Authorization", t)).andExpect(status().isForbidden());
        mvc.perform(get("/api/stats/reportes").header("Authorization", t)).andExpect(status().isForbidden());
        mvc.perform(get("/api/audit/search").header("Authorization", t)).andExpect(status().isForbidden());
    }

    @Test
    void historialSeFiltraPorUsuario() throws Exception {
        String t = login("admin_est"); // el login queda registrado en la auditoria
        mvc.perform(get("/api/audit/search?username=admin_est&accion=LOGIN_OK").header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("admin_est"))
                .andExpect(jsonPath("$.content[0].accion").value("LOGIN_OK"));
        mvc.perform(get("/api/audit/search?username=nadie_existe").header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void busquedaGlobalRespetaPermisos() throws Exception {
        mvc.perform(get("/api/search?q=a").header("Authorization", login("admin_est")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientes.length()").value(0)); // menos de 2 letras: sin resultados
        mvc.perform(get("/api/search?q=prueba").header("Authorization", login("empleado_est")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientes.length()").value(0))
                .andExpect(jsonPath("$.boletas.length()").value(0));
        mvc.perform(get("/api/search?q=prueba")).andExpect(status().isUnauthorized());
    }
}
