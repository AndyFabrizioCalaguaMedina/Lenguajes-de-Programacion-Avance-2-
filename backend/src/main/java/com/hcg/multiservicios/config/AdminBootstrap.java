package com.hcg.multiservicios.config;

import com.hcg.multiservicios.entity.Rol;
import com.hcg.multiservicios.entity.Usuario;
import com.hcg.multiservicios.repository.RolRepository;
import com.hcg.multiservicios.repository.UsuarioRepository;
import java.util.HashSet;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el primer ADMIN desde variables de entorno, solo si no existe ningun usuario.
 * La contraseña nunca esta en el codigo ni se escribe en logs.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String email;
    private final String password;

    public AdminBootstrap(UsuarioRepository usuarioRepository, RolRepository rolRepository, PasswordEncoder passwordEncoder,
                          @Value("${hcg.bootstrap.admin.username:}") String username,
                          @Value("${hcg.bootstrap.admin.email:}") String email,
                          @Value("${hcg.bootstrap.admin.password:}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        if (username.isBlank() || email.isBlank() || password.length() < 8) {
            log.warn("No hay usuarios. Defina HCG_ADMIN_USERNAME, HCG_ADMIN_EMAIL y HCG_ADMIN_PASSWORD (min. 8 caracteres) para crear el primer administrador.");
            return;
        }
        Usuario admin = new Usuario();
        admin.setUsername(username.trim().toLowerCase(Locale.ROOT));
        admin.setCorreo(email.trim().toLowerCase(Locale.ROOT));
        admin.setNombreCompleto("Administrador");
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRoles(new HashSet<>());
        admin.getRoles().add(rolRepository.findByNombre(Rol.ADMIN).orElseThrow());
        usuarioRepository.save(admin);
        log.info("Administrador inicial '{}' creado.", admin.getUsername());
    }
}
