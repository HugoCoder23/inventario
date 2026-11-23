package com.portafolio.inventario.service;

import com.portafolio.inventario.dto.LoginRequest;
import com.portafolio.inventario.dto.LoginResponse;
import com.portafolio.inventario.dto.UserResponse;
import com.portafolio.inventario.entity.User;
import com.portafolio.inventario.mapper.InventoryMapper;
import com.portafolio.inventario.repository.UserRepository;
import com.portafolio.inventario.security.CurrentUserProvider;
import com.portafolio.inventario.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final InventoryMapper mapper;
    private final CurrentUserProvider currentUser;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       InventoryMapper mapper, CurrentUserProvider currentUser) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(User::isActive)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                // Mismo mensaje para email inexistente, inactivo o contraseña incorrecta.
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        JwtService.IssuedToken token = jwtService.issue(user);
        return new LoginResponse(token.value(), "Bearer", token.expiresAt(), mapper.toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return mapper.toResponse(currentUser.get());
    }
}
