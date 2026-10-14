package com.hcg.multiservicios;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Clientes, proveedores, productos y servicios: validaciones, duplicados, busqueda, estado y permisos. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogosIntegrationTest {

    private static final String PASS = "Secreta123";
    private static final String RUC_VALIDO = "20100070970";
    private static final String RUC_VALIDO_2 = "20131312955";

    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired PermisoRepository permisos;
    @Autowired PasswordEncoder encoder;

    MockMvc mvc;
    String admin;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        crearUsuario("admin2", Rol.ADMIN);
        crearUsuario("empleado2", Rol.EMPLEADO);
        admin = login("admin2");
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
        String body = "{\"identificador\":\"" + username + "\",\"password\":\"" + PASS + "\"}";
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(res, "$.token");
    }

    private MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder b, String token, String json) {
        MockHttpServletRequestBuilder r = b.header("Authorization", "Bearer " + token);
        return json == null ? r : r.contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private String clienteRuc(String ruc, String razon) {
        return "{\"tipoDocumento\":\"RUC\",\"ruc\":\"" + ruc + "\",\"razonSocial\":\"" + razon + "\",\"telefono\":\"987654321\"}";
    }

    private int crearYObtenerId(String url, String json) throws Exception {
        MvcResult r = mvc.perform(conToken(post(url), admin, json)).andExpect(status().isCreated()).andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    // ---------- Clientes ----------

    @Test
    void crearClienteConRucValidoGeneraCodigo() throws Exception {
        mvc.perform(conToken(post("/api/clients"), admin, clienteRuc(RUC_VALIDO, "Corporacion Lima SAC")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(matchesPattern("CLI-\\d{6}")))
                .andExpect(jsonPath("$.nombre").value("Corporacion Lima SAC"))
                .andExpect(jsonPath("$.numeroDocumento").value(RUC_VALIDO))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void rucInvalidoDa400ConErrorEnElCampo() throws Exception {
        mvc.perform(conToken(post("/api/clients"), admin, clienteRuc("20512345678", "Empresa Falsa SAC")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.ruc").exists());
    }

    @Test
    void clienteConRucSinRazonSocialDa400() throws Exception {
        String json = "{\"tipoDocumento\":\"RUC\",\"ruc\":\"" + RUC_VALIDO + "\"}";
        mvc.perform(conToken(post("/api/clients"), admin, json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.razonSocial").exists());
    }

    @Test
    void rucDuplicadoDa409() throws Exception {
        crearYObtenerId("/api/clients", clienteRuc(RUC_VALIDO, "Primera SAC"));
        mvc.perform(conToken(post("/api/clients"), admin, clienteRuc(RUC_VALIDO, "Segunda SAC")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RUC"));
    }

    @Test
    void clientePersonaConDniArmaElNombre() throws Exception {
        String json = "{\"tipoDocumento\":\"DNI\",\"dni\":\"12345678\",\"nombres\":\"Maria\",\"apellidos\":\"Lopez Quispe\"}";
        mvc.perform(conToken(post("/api/clients"), admin, json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Maria Lopez Quispe"))
                .andExpect(jsonPath("$.numeroDocumento").value("12345678"));
        mvc.perform(conToken(post("/api/clients"), admin, json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_DNI"));
    }

    @Test
    void dniInvalidoDa400() throws Exception {
        String json = "{\"tipoDocumento\":\"DNI\",\"dni\":\"1234\",\"nombres\":\"Ana\",\"apellidos\":\"Ruiz\"}";
        mvc.perform(conToken(post("/api/clients"), admin, json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.dni").exists());
    }

    @Test
    void buscarClientePorRucNombreYEstado() throws Exception {
        int id = crearYObtenerId("/api/clients", clienteRuc(RUC_VALIDO, "Ferreteria Central SRL"));
        crearYObtenerId("/api/clients", clienteRuc(RUC_VALIDO_2, "Muebleria El Cedro SAC"));

        mvc.perform(conToken(get("/api/clients?q=ferret"), admin, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].ruc").value(RUC_VALIDO));
        mvc.perform(conToken(get("/api/clients?q=" + RUC_VALIDO_2), admin, null))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nombre").value("Muebleria El Cedro SAC"));

        mvc.perform(conToken(patch("/api/clients/" + id + "/status"), admin, "{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(conToken(get("/api/clients?q=ferret&activo=true"), admin, null))
                .andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(conToken(get("/api/clients?q=ferret&activo=false"), admin, null))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void editarClienteYClienteInexistente() throws Exception {
        int id = crearYObtenerId("/api/clients", clienteRuc(RUC_VALIDO, "Nombre Viejo SAC"));
        mvc.perform(conToken(put("/api/clients/" + id), admin, clienteRuc(RUC_VALIDO, "Nombre Nuevo SAC")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre Nuevo SAC"));
        mvc.perform(conToken(get("/api/clients/999999"), admin, null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"));
    }

    // ---------- Permisos ----------

    @Test
    void empleadoSinPermisoRecibe403AunConCuerpoInvalido() throws Exception {
        String token = login("empleado2");
        mvc.perform(conToken(get("/api/clients"), token, null)).andExpect(status().isForbidden());
        mvc.perform(conToken(post("/api/clients"), token, "{}")).andExpect(status().isForbidden());
    }

    @Test
    void empleadoConVerPuedeListarPeroNoCrear() throws Exception {
        Rol empleado = roles.findByNombre(Rol.EMPLEADO).orElseThrow();
        Permiso ver = permisos.findByCodigoIn(List.of("CLIENTES_VER")).get(0);
        empleado.getPermisos().add(ver);
        roles.saveAndFlush(empleado);
        String token = login("empleado2");
        mvc.perform(conToken(get("/api/clients"), token, null)).andExpect(status().isOk());
        mvc.perform(conToken(post("/api/clients"), token, clienteRuc(RUC_VALIDO, "X SAC")))
                .andExpect(status().isForbidden());
    }

    // ---------- Proveedores ----------

    @Test
    void proveedorRequiereRucValidoYNoSeDuplica() throws Exception {
        String ok = "{\"ruc\":\"" + RUC_VALIDO + "\",\"razonSocial\":\"Distribuciones Andinas EIRL\",\"contacto\":\"Juan Perez\"}";
        mvc.perform(conToken(post("/api/providers"), admin, ok))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.razonSocial").value("Distribuciones Andinas EIRL"))
                .andExpect(jsonPath("$.activo").value(true));
        mvc.perform(conToken(post("/api/providers"), admin, ok))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RUC"));
        mvc.perform(conToken(post("/api/providers"), admin, "{\"razonSocial\":\"Sin Ruc SAC\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.ruc").exists());
        mvc.perform(conToken(post("/api/providers"), admin, "{\"ruc\":\"20512345678\",\"razonSocial\":\"Ruc Malo SAC\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.ruc").exists());
    }

    @Test
    void desactivarProveedor() throws Exception {
        int id = crearYObtenerId("/api/providers", "{\"ruc\":\"" + RUC_VALIDO + "\",\"razonSocial\":\"Proveedor Uno SAC\"}");
        mvc.perform(conToken(patch("/api/providers/" + id + "/status"), admin, "{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    // ---------- Productos y servicios ----------

    @Test
    void productoSeCreaConCodigoAutomaticoYValoresPorDefecto() throws Exception {
        String json = "{\"nombre\":\"Cuaderno A4\",\"categoria\":\"Escolar\",\"precioVenta\":4.50,\"precioCompra\":3.00,\"stock\":10,\"stockMinimo\":15}";
        mvc.perform(conToken(post("/api/products"), admin, json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(matchesPattern("PRD-\\d{6}")))
                .andExpect(jsonPath("$.tipo").value("PRODUCTO"))
                .andExpect(jsonPath("$.unidadMedida").value("UND"))
                .andExpect(jsonPath("$.stockBajo").value(true));
    }

    @Test
    void precioNegativoOStockNegativoDa400() throws Exception {
        String json = "{\"nombre\":\"Lapicero\",\"precioVenta\":-1,\"stock\":-5}";
        mvc.perform(conToken(post("/api/products"), admin, json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.precioVenta").exists())
                .andExpect(jsonPath("$.errors.stock").exists());
    }

    @Test
    void codigoDuplicadoDa409() throws Exception {
        String json = "{\"codigo\":\"ABC-1\",\"nombre\":\"Item\",\"precioVenta\":1}";
        mvc.perform(conToken(post("/api/products"), admin, json)).andExpect(status().isCreated());
        mvc.perform(conToken(post("/api/products"), admin, json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_CODE"));
    }

    @Test
    void servicioNoAceptaStockYSeSeparaDeLosProductos() throws Exception {
        mvc.perform(conToken(post("/api/services"), admin, "{\"nombre\":\"Impresion A4\",\"precioVenta\":0.30,\"stock\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.stock").exists());

        int idServicio = crearYObtenerId("/api/services", "{\"nombre\":\"Impresion A4 color\",\"precioVenta\":0.50}");
        crearYObtenerId("/api/products", "{\"nombre\":\"Impresion de prueba producto\",\"precioVenta\":1}");

        mvc.perform(conToken(get("/api/services?q=impresion"), admin, null))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].tipo").value("SERVICIO"))
                .andExpect(jsonPath("$.content[0].codigo").value(matchesPattern("SRV-\\d{6}")))
                .andExpect(jsonPath("$.content[0].stock").doesNotExist());
        mvc.perform(conToken(get("/api/products?q=impresion"), admin, null))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].tipo").value("PRODUCTO"));
        // un servicio no es accesible como producto
        mvc.perform(conToken(get("/api/products/" + idServicio), admin, null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void desactivarProductoLoExcluyeDelFiltroDeActivos() throws Exception {
        int id = crearYObtenerId("/api/products", "{\"nombre\":\"Borrador\",\"precioVenta\":1}");
        mvc.perform(conToken(patch("/api/products/" + id + "/status"), admin, "{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(conToken(get("/api/products?q=borrador&activo=true"), admin, null))
                .andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(conToken(get("/api/products?q=borrador&activo=false"), admin, null))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }
}
