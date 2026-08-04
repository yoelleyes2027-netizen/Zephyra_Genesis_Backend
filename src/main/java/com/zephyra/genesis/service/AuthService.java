package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.AuthUserResponse;
import com.zephyra.genesis.dto.LoginRequest;
import com.zephyra.genesis.dto.RegisterRequest;
import com.zephyra.genesis.entity.PersonaEntity;
import com.zephyra.genesis.entity.ROL;
import com.zephyra.genesis.entity.UsuarioEntity;
import com.zephyra.genesis.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (usuarioRepository.existsByCedula(request.cedula())) {
            throw new IllegalArgumentException("Ya existe un usuario con esa cédula.");
        }

        ROL rol = ROL.valueOf(request.rol().trim().toUpperCase());
        String email = request.email() != null && !request.email().isBlank()
                ? request.email().trim()
                : request.cedula() + "@zephyra.local";
        int telefono = request.telefono() != null ? request.telefono() : 0;

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setName(request.nombre());
        usuario.setEmail(email);
        usuario.setTelefono(telefono);
        usuario.setCedula(request.cedula());
        usuario.setPassword(passwordEncoder.encode(request.contraseña()));
        usuario.setRol(rol);
        usuario.setFechaCreacion(new Date());
        usuario.setFotoPerfil(null);
        usuario.setFechaInicioDeDia(null);

        UsuarioEntity saved = usuarioRepository.save(usuario);
        return new AuthResult(jwtService.generateToken(saved), toResponse(saved));
    }

    public AuthResult login(LoginRequest request) {
        UsuarioEntity usuario = usuarioRepository.findByCedula(request.cedula())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (!passwordEncoder.matches(request.contraseña(), usuario.getPassword())) {
            throw new IllegalArgumentException("Contraseña incorrecta.");
        }

        return new AuthResult(jwtService.generateToken(usuario), toResponse(usuario));
    }

    public AuthUserResponse validarToken(String token) {
        if (token == null || token.isBlank() || !jwtService.isValid(token)) {
            throw new IllegalArgumentException("Token inválido.");
        }

        var claims = jwtService.parseClaims(token);
        Long id = Long.valueOf(claims.getSubject());
        String nombre = claims.get("nombre", String.class);
        String rol = claims.get("rol", String.class);
        Integer cedula = claims.get("cedula", Integer.class);
        String tenantDatabase = claims.get("tenantDatabase", String.class);
        return new AuthUserResponse(id, nombre, rol, cedula != null ? cedula : 0, tenantDatabase);
    }

    private AuthUserResponse toResponse(UsuarioEntity usuario) {
        String nombre = usuario.getName() != null ? usuario.getName() : "";
        String rol = usuario.getRol() != null ? usuario.getRol().name().toLowerCase() : "";
        return new AuthUserResponse(usuario.getId(), nombre, rol, usuario.getCedula(), usuario.getTenantDatabase());
    }

    public record AuthResult(String token, AuthUserResponse user) {
    }
}
