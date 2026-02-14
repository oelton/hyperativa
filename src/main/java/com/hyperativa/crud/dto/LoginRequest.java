package com.hyperativa.crud.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "Nome de usuário", example = "admin")
        @NotBlank String username,
        @Schema(description = "Senha do usuário", example = "123456")
        @NotBlank String password
) {
}
