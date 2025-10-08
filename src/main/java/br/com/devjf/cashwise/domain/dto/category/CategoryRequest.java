package br.com.devjf.cashwise.domain.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank
        @Size(max = 100, message = "O nome da categoria deve conter no máximo 100 caracteres")
        String name
        ) {

}
