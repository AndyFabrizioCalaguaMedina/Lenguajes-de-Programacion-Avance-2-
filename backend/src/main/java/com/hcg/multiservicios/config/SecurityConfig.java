package com.hcg.multiservicios.config;

import com.hcg.multiservicios.security.JsonErrorWriterAccess;
import com.hcg.multiservicios.security.UsuarioJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, UsuarioJwtAuthenticationConverter converter) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API stateless con Bearer token: no hay cookies de sesion que proteger
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Permisos tambien a nivel de URL: asi el 403 se decide ANTES de validar el cuerpo
                        // (si no, quien no tiene permiso veria errores de validacion en vez de 403).
                        // Los @PreAuthorize de los controllers se mantienen como segunda barrera.
                        .requestMatchers(HttpMethod.POST, "/api/users").hasAuthority("USUARIOS_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/users/*/roles", "/api/users/*/password").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/*").hasAuthority("USUARIOS_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/status").hasAuthority("USUARIOS_EDITAR")
                        .requestMatchers(HttpMethod.POST, "/api/roles").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/roles/*/permissions").hasRole("ADMIN")
                        // Boletas: ya no son publicas; permisos tambien a nivel de URL
                        .requestMatchers(HttpMethod.POST, "/api/v1/boletas/emitir", "/api/v1/boletas/*/reenviar").hasAuthority("COMPROBANTES_CREAR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/boletas", "/api/v1/boletas/**").hasAuthority("COMPROBANTES_VER")
                        // Cotizaciones y documentos
                        .requestMatchers(HttpMethod.GET, "/api/quotations", "/api/quotations/**").hasAuthority("COTIZACIONES_VER")
                        .requestMatchers(HttpMethod.POST, "/api/quotations").hasAuthority("COTIZACIONES_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/quotations/*").hasAuthority("COTIZACIONES_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/quotations/*/status").hasAuthority("COTIZACIONES_EDITAR")
                        .requestMatchers(HttpMethod.POST, "/api/quotations/*/convertir-boleta").hasAuthority("COMPROBANTES_CREAR")
                        .requestMatchers(HttpMethod.GET, "/api/documents", "/api/documents/**").hasAuthority("DOCUMENTOS_VER")
                        .requestMatchers(HttpMethod.POST, "/api/documents").hasAuthority("DOCUMENTOS_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/documents/*").hasAuthority("DOCUMENTOS_EDITAR")
                        .requestMatchers(HttpMethod.POST, "/api/documents/*/anular").hasAuthority("DOCUMENTOS_ANULAR")
                        // Salidas de bodega
                        .requestMatchers(HttpMethod.GET, "/api/warehouse-exits", "/api/warehouse-exits/**").hasAuthority("BODEGA_VER")
                        .requestMatchers(HttpMethod.POST, "/api/warehouse-exits").hasAuthority("BODEGA_CREAR")
                        .requestMatchers(HttpMethod.POST, "/api/warehouse-exits/*/anular").hasAuthority("BODEGA_ANULAR")
                        // Operaciones (ventas, compras, pedidos)
                        .requestMatchers(HttpMethod.GET, "/api/operations", "/api/operations/**").hasAuthority("OPERACIONES_VER")
                        .requestMatchers(HttpMethod.POST, "/api/operations").hasAuthority("OPERACIONES_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/operations/*").hasAuthority("OPERACIONES_EDITAR")
                        .requestMatchers(HttpMethod.POST, "/api/operations/*/events").hasAuthority("OPERACIONES_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/operations/*/status").hasAuthority("OPERACIONES_EDITAR")
                        // Catalogos: permisos por modulo tambien a nivel de URL
                        .requestMatchers(HttpMethod.GET, "/api/clients", "/api/clients/**").hasAuthority("CLIENTES_VER")
                        .requestMatchers(HttpMethod.POST, "/api/clients").hasAuthority("CLIENTES_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/clients/*").hasAuthority("CLIENTES_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/clients/*/status").hasAuthority("CLIENTES_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/clients/*").hasAuthority("CLIENTES_EDITAR")
                        .requestMatchers(HttpMethod.GET, "/api/providers", "/api/providers/**").hasAuthority("PROVEEDORES_VER")
                        .requestMatchers(HttpMethod.POST, "/api/providers").hasAuthority("PROVEEDORES_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/providers/*").hasAuthority("PROVEEDORES_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/providers/*/status").hasAuthority("PROVEEDORES_EDITAR")
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").hasAuthority("PRODUCTOS_VER")
                        .requestMatchers(HttpMethod.POST, "/api/products").hasAuthority("PRODUCTOS_CREAR")
                        .requestMatchers(HttpMethod.POST, "/api/products/import").hasAuthority("PRODUCTOS_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/products/*").hasAuthority("PRODUCTOS_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/products/*/status").hasAuthority("PRODUCTOS_EDITAR")
                        .requestMatchers(HttpMethod.GET, "/api/services", "/api/services/**").hasAuthority("PRODUCTOS_VER")
                        .requestMatchers(HttpMethod.POST, "/api/services").hasAuthority("PRODUCTOS_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/services/*").hasAuthority("PRODUCTOS_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/services/*/status").hasAuthority("PRODUCTOS_EDITAR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(JsonErrorWriterAccess.entryPoint())
                        .accessDeniedHandler(JsonErrorWriterAccess.accessDeniedHandler()))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(JsonErrorWriterAccess.entryPoint())
                        .accessDeniedHandler(JsonErrorWriterAccess.accessDeniedHandler()));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}

