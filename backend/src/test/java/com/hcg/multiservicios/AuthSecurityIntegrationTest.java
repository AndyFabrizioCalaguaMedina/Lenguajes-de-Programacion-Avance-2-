package com.hcg.multiservicios;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hcg.multiservicios.entity.Permiso;
import com.hcg.multiservicios.entity.Rol;
import com.hcg.multiservicios.entity.Usuario;
import com.hcg.multiservicios.repository.PermisoRepository;
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

/** Login, autorizacion por permisos, bloqueo e invalidacion inmediata al desactivar. Cada test se revierte. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthSecurityIntegrationTest {

    private static final String PASS = "Secreta123";

    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired PermisoRepository permisos;
    @Autowired PasswordEncoder encoder;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        crearUsuario("admin1", Rol.ADMIN);
        crearUsuario("empleado1", Rol.EMPLEADO);
    }

    private Usuario crearUsuario(String username, String rol) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setCorreo(username + "@hcg.pe");
        u.setNombreCompleto(username);
        u.setPasswordHash(encoder.encode(PASS));
        u.setRoles(new HashSet<>(List.of(roles.findByNombre(rol).orElseThrow())));
        return usuarios.saveAndFlush(u);
    }

    private String login(String identificador, String password) throws Exception {
        String body = "{\"identificador\":\"" + identificador + "\",\"password\":\"" + password + "\"}";
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(res, "$.token");
    }

    @Test
    void loginCorrectoDevuelveTokenYUsuarioSinHash() throws Exception {
        String body = "{\"identificador\":\"ADMIN1\",\"password\":\"" + PASS + "\"}"; // insensible a mayusculas
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.username").value("admin1"))
                .andExpect(jsonPath("$.usuario.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist());
    }

    @Test
    void passwordIncorrectaDa401ConCodigoEstable() throws Exception {
        String body = "{\"identificador\":\"admin1\",\"password\":\"mala\"}";
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginSinCamposDa400Validacion() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void sinTokenDa401Json() throws Exception {
        mvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void tokenFalsoDa401() throws Exception {
        mvc.perform(get("/api/users").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void empleadoSinPermisoDa403() throws Exception {
        String token = login("empleado1", PASS);
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void empleadoConPermisoUsuariosVerPuedeListarPeroNoCrear() throws Exception {
        Rol empleado = roles.findByNombre(Rol.EMPLEADO).orElseThrow();
        Permiso ver = permisos.findByCodigoIn(List.of("USUARIOS_VER")).get(0);
        empleado.getPermisos().add(ver);
        roles.saveAndFlush(empleado);

        String token = login("empleado1", PASS);
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        String nuevo = "{\"username\":\"xyz1\",\"nombreCompleto\":\"X\",\"correo\":\"xyz1@hcg.pe\",\"password\":\"Secreta123\",\"roles\":[\"EMPLEADO\"]}";
        mvc.perform(post("/api/users").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(nuevo))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreaUsuarioYDuplicadoDa409() throws Exception {
        String token = login("admin1", PASS);
        String nuevo = "{\"username\":\"Maria\",\"nombreCompleto\":\"Maria L\",\"correo\":\"Maria@HCG.pe\",\"password\":\"Secreta123\",\"roles\":[\"EMPLEADO\"]}";
        mvc.perform(post("/api/users").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(nuevo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("maria"))
                .andExpect(jsonPath("$.correo").value("maria@hcg.pe"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(post("/api/users").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(nuevo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USERNAME_TAKEN"));
    }

    @Test
    void crearUsuarioConRolInexistenteDa400() throws Exception {
        String token = login("admin1", PASS);
        String nuevo = "{\"username\":\"zz1\",\"nombreCompleto\":\"Z\",\"correo\":\"zz1@hcg.pe\",\"password\":\"Secreta123\",\"roles\":[\"NOEXISTE\"]}";
        mvc.perform(post("/api/users").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(nuevo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ROLE_NOT_FOUND"));
    }

    @Test
    void desactivarUsuarioInvalidaSuTokenDeInmediato() throws Exception {
        String tokenEmpleado = login("empleado1", PASS);
        String tokenAdmin = login("admin1", PASS);
        Long id = usuarios.findByUsernameOrCorreo("empleado1", "empleado1").orElseThrow().getId();

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenEmpleado)).andExpect(status().isOk());
        mvc.perform(patch("/api/users/" + id + "/status").header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenEmpleado)).andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioInactivoNoPuedeIniciarSesion() throws Exception {
        Usuario u = usuarios.findByUsernameOrCorreo("empleado1", "empleado1").orElseThrow();
        u.setActivo(false);
        usuarios.saveAndFlush(u);
        String body = "{\"identificador\":\"empleado1\",\"password\":\"" + PASS + "\"}";
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("USER_INACTIVE"));
    }

    @Test
    void cincoIntentosFallidosBloqueanLaCuenta() throws Exception {
        String mala = "{\"identificador\":\"empleado1\",\"password\":\"incorrecta\"}";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(mala))
                    .andExpect(status().isUnauthorized());
        }
        // Ahora ni siquiera la contraseña correcta entra
        String buena = "{\"identificador\":\"empleado1\",\"password\":\"" + PASS + "\"}";
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(buena))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"));
    }

    @Test
    void noSePuedeDesactivarAlUnicoAdminNiASiMismo() throws Exception {
        String token = login("admin1", PASS);
        Long id = usuarios.findByUsernameOrCorreo("admin1", "admin1").orElseThrow().getId();
        mvc.perform(patch("/api/users/" + id + "/status").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CANNOT_DEACTIVATE_SELF"));
    }

    @Test
    void adminPuedeConsultarAuditoriaYRegistraElLogin() throws Exception {
        String token = login("admin1", PASS);
        mvc.perform(get("/api/audit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accion").value("LOGIN_OK"));
    }

    @Test
    void rutaInexistenteDa404ConFormatoComun() throws Exception {
        String token = login("admin1", PASS);
        mvc.perform(get("/api/no-existe").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
    @Test
    void corsPermiteAmbosFrontendsYTodosLosMetodos() throws Exception {
        for (String origin : List.of("http://localhost:3000", "http://localhost:5173")) {
            for (String method : List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")) {
                mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/clients")
                                .header("Origin", origin)
                                .header("Access-Control-Request-Method", method)
                                .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                        .andExpect(status().isOk())
                        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                                .string("Access-Control-Allow-Origin", origin));
            }
        }
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/clients")
                        .header("Origin", "https://no-permitido.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void swaggerEsPublicoYDescribeBearerYLogin() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/quotations'].post").exists());
    }
}


