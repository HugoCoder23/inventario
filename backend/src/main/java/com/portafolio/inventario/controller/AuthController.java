package com.portafolio.inventario.controller;

import com.portafolio.inventario.dto.LoginRequest;
import com.portafolio.inventario.dto.LoginResponse;
import com.portafolio.inventario.dto.UserResponse;
import com.portafolio.inventario.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements // público
    @Operation(summary = "Autenticar usuario y obtener un JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Datos del usuario autenticado")
    public UserResponse me() {
        return authService.me();
    }
}
