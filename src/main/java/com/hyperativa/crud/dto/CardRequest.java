package com.hyperativa.crud.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CardRequest(
        @Schema(description = "Número completo do cartão", example = "4111111111111111")
        @NotBlank(message = "Número do cartão é obrigatório")
        @Size(min = 13, max = 19, message = "Número do cartão inválido")
        String cardNumber
) {
}
