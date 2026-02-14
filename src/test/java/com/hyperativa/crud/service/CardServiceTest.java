package com.hyperativa.crud.service;

import com.hyperativa.crud.domain.model.Card;
import com.hyperativa.crud.domain.repository.CardRepository;
import com.hyperativa.crud.exception.FileProcessingException;
import com.hyperativa.crud.exception.HashGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    public static final String CARD_NUMBER = "1234567890123";
    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    private static final String TEST_SECRET = "test-secret-key-at-least-256-bits-long";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cardService, "secret", TEST_SECRET);
    }

    @Test
    @DisplayName("Deve salvar um novo cartão com sucesso")
    void saveCardNewSuccess() {
        when(cardRepository.findByCardNumberHash(anyString())).thenReturn(Optional.empty());
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0);
            return Card.builder()
                    .id(1L)
                    .cardNumberHash(card.getCardNumberHash())
                    .encryptedCardNumber(card.getEncryptedCardNumber())
                    .build();
        });

        Long id = cardService.saveCard(CARD_NUMBER);

        assertThat(id).isEqualTo(1L);
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("Deve retornar ID de cartão existente ao tentar salvar duplicata")
    void saveCardExistingSuccess() {
        Card existingCard = Card.builder().id(10L).build();
        when(cardRepository.findByCardNumberHash(anyString())).thenReturn(Optional.of(existingCard));

        Long id = cardService.saveCard(CARD_NUMBER);

        assertThat(id).isEqualTo(10L);
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    @DisplayName("Deve encontrar ID do cartão pelo número")
    void findCardIdFound() {
        Card existingCard = Card.builder().id(5L).build();
        when(cardRepository.findByCardNumberHash(anyString())).thenReturn(Optional.of(existingCard));

        Optional<Long> result = cardService.findCardId(CARD_NUMBER);

        assertThat(result).isPresent().contains(5L);
    }

    @Test
    @DisplayName("Deve retornar vazio ao buscar cartão inexistente")
    void findCardIdNotFound() {
        when(cardRepository.findByCardNumberHash(anyString())).thenReturn(Optional.empty());

        Optional<Long> result = cardService.findCardId(CARD_NUMBER);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Deve processar arquivo TXT com múltiplos cartões")
    void processFileSuccess(){
        String content = "1111111111111\n2222222222222\n\n3333333333333";
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", content.getBytes());
        
        when(cardRepository.findByCardNumberHash(anyString())).thenReturn(Optional.empty());
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> Card.builder().id(1L).build());

        Long count = cardService.processFile(file);

        assertThat(count).isEqualTo(3L);
        verify(cardRepository, times(3)).save(any(Card.class));
    }

    @Test
    @DisplayName("Deve lançar FileProcessingException ao ocorrer erro de leitura")
    void processFileError() throws IOException {
        MultipartFileMock fileMock = mock(MultipartFileMock.class);
        when(fileMock.getInputStream()).thenThrow(new IOException("Simulated error"));

        assertThatThrownBy(() -> cardService.processFile(fileMock))
                .isInstanceOf(FileProcessingException.class)
                .hasMessageContaining("Falha ao processar arquivo");
    }

    @Test
    @DisplayName("Deve lançar HashGenerationException quando MessageDigest.getInstance falhar")
    void saveCardHashGenerationError() {

        try (var mockedStatic = mockStatic(MessageDigest.class)) {
            mockedStatic.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new NoSuchAlgorithmException("Algorithm not found"));

            assertThatThrownBy(() -> cardService.saveCard(CARD_NUMBER))
                    .isInstanceOf(HashGenerationException.class)
                    .hasMessageContaining("Erro ao gerar hash do cartão")
                    .hasCauseInstanceOf(NoSuchAlgorithmException.class);
        }
    }

    interface MultipartFileMock extends org.springframework.web.multipart.MultipartFile {}
}
