package com.example.SportFlow.Dto.Pagamento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PagamentoUpdateRequest(
        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "Valor inválido (máx. 8 dígitos e 2 casas decimais)")
        BigDecimal valor,

        @NotBlank(message = "A forma de pagamento é obrigatória")
        @Size(max = 30, message = "A forma de pagamento deve ter no máximo 30 caracteres")
        String forma
) {}
