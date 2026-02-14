package com.hyperativa.crud.controller;

import com.hyperativa.crud.dto.CardRequest;
import com.hyperativa.crud.dto.CardResponse;
import com.hyperativa.crud.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
@Tag(name = "Cartões", description = "Endpoints para gerenciamento de cartões (inserção e consulta)")
public class CardController {

    private final CardService cardService;

    @PostMapping
    @Operation(summary = "Insere um único cartão", description = "Realiza o cadastro de um número de cartão completo no banco de dados de forma segura")
    public ResponseEntity<CardResponse> create(@RequestBody @Valid CardRequest request) {
        Long id = cardService.saveCard(request.cardNumber());
        return ResponseEntity.ok(new CardResponse(id));
    }

    @PostMapping("/upload")
    @Operation(summary = "Insere cartões via arquivo TXT", description = "Processa um arquivo TXT contendo um número de cartão completo por linha")
    public ResponseEntity<String> upload(@Parameter(description = "Arquivo TXT com números de cartões") @RequestParam("file") MultipartFile file) {
        Long processed = cardService.processFile(file);
        return ResponseEntity.ok("Registros processados: " + processed);
    }

    @GetMapping("/exists")
    @Operation(
            summary = "Consulta se um cartão existe",
            description = "Verifica se o número de cartão completo informado existe na base de dados",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cartão encontrado"),
                    @ApiResponse(responseCode = "404", description = "Cartão não encontrado", content = @Content)
            }
    )
    public ResponseEntity<CardResponse> exists(@Parameter(description = "Número completo do cartão para consulta") @RequestParam("number") String number) {
        return cardService.findCardId(number)
                .map(id -> ResponseEntity.ok(new CardResponse(id)))
                .orElse(ResponseEntity.notFound().build());
    }
}
