package com.hyperativa.crud.controller;

import com.hyperativa.crud.domain.model.User;
import com.hyperativa.crud.domain.repository.UserRepository;
import com.hyperativa.crud.dto.LoginRequest;
import com.hyperativa.crud.dto.LoginResponse;
import com.hyperativa.crud.security.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints para registro e login de usuários")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    @Operation(summary = "Realiza o login do usuário", description = "Retorna um token JWT válido para autenticação nos demais endpoints")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest data) {
        try {
            var usernamePassword = new UsernamePasswordAuthenticationToken(data.username(), data.password());
            var auth = this.authenticationManager.authenticate(usernamePassword);
            var token = tokenService.generateToken((User) auth.getPrincipal());
            return ResponseEntity.ok(new LoginResponse(token));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Registra um novo usuário", description = "Cria um novo usuário para acesso à API")
    public ResponseEntity<Void> register(@RequestBody @Valid LoginRequest data) {
        if (this.userRepository.findByUsername(data.username()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        String encryptedPassword = passwordEncoder.encode(data.password());
        User newUser = User.builder()
                .username(data.username())
                .password(encryptedPassword)
                .build();
        this.userRepository.save(newUser);
        return ResponseEntity.ok().build();
    }
}
