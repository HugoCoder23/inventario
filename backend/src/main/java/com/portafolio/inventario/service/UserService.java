package com.portafolio.inventario.service;

import com.portafolio.inventario.dto.UserRequest;
import com.portafolio.inventario.dto.UserResponse;
import com.portafolio.inventario.entity.Role;
import com.portafolio.inventario.entity.User;
import com.portafolio.inventario.exception.BusinessRuleException;
import com.portafolio.inventario.exception.ConflictException;
import com.portafolio.inventario.exception.ResourceNotFoundException;
import com.portafolio.inventario.mapper.InventoryMapper;
import com.portafolio.inventario.repository.UserRepository;
import com.portafolio.inventario.security.CurrentUserProvider;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InventoryMapper mapper;
    private final CurrentUserProvider currentUser;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, InventoryMapper mapper,
                       CurrentUserProvider currentUser) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAll(Sort.by("name")).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("DUPLICATE_EMAIL", "Ya existe un usuario con el email " + email);
        }
        if (!StringUtils.hasText(request.password())) {
            throw new BusinessRuleException("PASSWORD_REQUIRED", "La contraseña es obligatoria para un usuario nuevo");
        }
        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()), request.role());
        if (request.active() != null) user.setActive(request.active());
        return mapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("DUPLICATE_EMAIL", "Ya existe un usuario con el email " + email);
        }
        boolean active = request.active() == null ? user.isActive() : request.active();
        if (user.getId().equals(currentUser.get().getId()) && (!active || request.role() != Role.ADMIN)) {
            throw new BusinessRuleException("SELF_LOCKOUT", "No puedes quitarte el rol de administrador ni desactivarte");
        }
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setRole(request.role());
        user.setActive(active);
        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return mapper.toResponse(user);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
