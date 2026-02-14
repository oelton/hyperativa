package com.hyperativa.crud.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CardResponse(
        @Schema(description = "Identificador único do cartão no sistema", example = "1")
        Long id
) {
}
