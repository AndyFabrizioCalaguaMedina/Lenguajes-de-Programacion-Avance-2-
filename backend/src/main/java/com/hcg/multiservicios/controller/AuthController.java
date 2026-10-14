package com.hcg.multiservicios.controller;

import com.hcg.multiservicios.dto.security.ChangePasswordRequest;
import com.hcg.multiservicios.dto.security.LoginRequest;
import com.hcg.multiservicios.dto.security.LoginResponse;
import com.hcg.multiservicios.dto.security.UsuarioDto;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UsuarioDto me(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return authService.me(principal);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> cambiarPassword(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                @Valid @RequestBody ChangePasswordRequest request) {
        authService.cambiarPassword(principal, request);
        return ResponseEntity.noContent().build();
    }
}
