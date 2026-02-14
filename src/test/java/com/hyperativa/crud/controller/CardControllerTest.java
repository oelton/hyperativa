package com.hyperativa.crud.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperativa.crud.dto.CardRequest;
import com.hyperativa.crud.service.CardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class CardControllerTest {

    public static final String CARD_NUMBER = "1234567890123456";
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @Test
    @DisplayName("POST /cards - Deve criar um cartão com sucesso")
    void createCardSuccess() throws Exception {
        CardRequest request = new CardRequest(CARD_NUMBER);
        when(cardService.saveCard(anyString())).thenReturn(1L);

        mockMvc.perform(post("/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("POST /cards - Deve retornar erro 400 quando cardNumber for inválido")
    void createCardInvalidNumber() throws Exception {
        CardRequest request = new CardRequest("");

        mockMvc.perform(post("/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /cards/upload - Deve fazer upload de arquivo com sucesso")
    void uploadFileSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "1234567890123456\n9876543210987654".getBytes()
        );
        when(cardService.processFile(any())).thenReturn(2L);

        mockMvc.perform(multipart("/cards/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Registros processados: 2"));
    }

    @Test
    @DisplayName("GET /cards/exists - Deve retornar 200 quando cartão existe")
    void existsCardFound() throws Exception {
        when(cardService.findCardId(anyString())).thenReturn(Optional.of(5L));

        mockMvc.perform(get("/cards/exists")
                        .param("number", CARD_NUMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L));
    }

    @Test
    @DisplayName("GET /cards/exists - Deve retornar 404 quando cartão não existe")
    void existsCardNotFound() throws Exception {
        when(cardService.findCardId(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/cards/exists")
                        .param("number", CARD_NUMBER))
                .andExpect(status().isNotFound());
    }
}
