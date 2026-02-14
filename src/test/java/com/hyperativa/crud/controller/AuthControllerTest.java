package com.hyperativa.crud.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperativa.crud.domain.model.User;
import com.hyperativa.crud.domain.repository.UserRepository;
import com.hyperativa.crud.dto.LoginRequest;
import com.hyperativa.crud.security.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    public static final String AUTH_LOGIN = "/auth/login";
    public static final String AUTH_REGISTER = "/auth/register";
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("POST /auth/login - Deve fazer login com sucesso")
    void loginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("user", "password");
        User user = User.builder().username("user").password("encoded").build();
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenService.generateToken(any(User.class))).thenReturn("mock-jwt-token");

        mockMvc.perform(post(AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"));
    }

    @Test
    @DisplayName("POST /auth/login - Deve retornar 401 quando credenciais inválidas")
    void loginUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest("user", "wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post(AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login - Deve retornar 400 quando dados faltando")
    void loginBadRequest() throws Exception {
        LoginRequest request = new LoginRequest("", "");

        mockMvc.perform(post(AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register - Deve registrar novo usuário com sucesso")
    void registerSuccess() throws Exception {
        LoginRequest request = new LoginRequest("newuser", "password123");

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(User.builder().id(1L).build());

        mockMvc.perform(post(AUTH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/register - Deve retornar 400 quando usuário já existe")
    void registerUserAlreadyExists() throws Exception {
        LoginRequest request = new LoginRequest("existinguser", "password123");
        User existingUser = User.builder().username("existinguser").build();

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(existingUser));

        mockMvc.perform(post(AUTH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register - Deve retornar 400 quando dados inválidos")
    void registerInvalidData() throws Exception {
        LoginRequest request = new LoginRequest("", "");

        mockMvc.perform(post(AUTH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
