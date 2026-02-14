package com.hyperativa.crud.service;

import com.hyperativa.crud.domain.model.Card;
import com.hyperativa.crud.domain.repository.CardRepository;
import com.hyperativa.crud.exception.FileProcessingException;
import com.hyperativa.crud.exception.HashGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;

    @Value("${api.security.token.secret}")
    private String secret;

    private TextEncryptor getEncryptor() {
        return Encryptors.text(secret, "deadbeef"); 
    }

    public Long saveCard(String cardNumber) {
        String hash = hashCardNumber(cardNumber);
        Optional<Card> existing = cardRepository.findByCardNumberHash(hash);
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        String encrypted = getEncryptor().encrypt(cardNumber);
        Card card = Card.builder()
                .cardNumberHash(hash)
                .encryptedCardNumber(encrypted)
                .build();
        
        return cardRepository.save(card).getId();
    }

    public Long processFile(MultipartFile file) {
        long count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    saveCard(line.trim());
                    count++;
                }
            }
        } catch (Exception e) {
            log.error("Erro ao processar arquivo TXT", e);
            throw new FileProcessingException("Falha ao processar arquivo");
        }
        return count;
    }

    public Optional<Long> findCardId(String cardNumber) {
        String hash = hashCardNumber(cardNumber);
        return cardRepository.findByCardNumberHash(hash).map(Card::getId);
    }

    private String hashCardNumber(String cardNumber) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(cardNumber.getBytes());
            return Base64.getEncoder().encodeToString(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new HashGenerationException("Erro ao gerar hash do cartão", e);
        }
    }
}
