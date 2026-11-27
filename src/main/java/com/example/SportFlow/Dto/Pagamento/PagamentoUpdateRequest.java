package com.example.SportFlow.Dto.Pagamento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

public record PagamentoUpdateRequest(
        /**
         * Opcional. O valor do pagamento é sempre o valor da reserva (valorTotal).
         * Se vier preenchido, precisa ser igual a ele; senão a API responde 400.
         */
        @Schema(description = "Opcional: se informado, deve ser igual ao valorTotal da reserva", example = "120.00", nullable = true)
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "Valor inválido (máx. 8 dígitos e 2 casas decimais)")
        BigDecimal valor,

        @NotBlank(message = "A forma de pagamento é obrigatória")
        @Size(max = 30, message = "A forma de pagamento deve ter no máximo 30 caracteres")
        String forma
) {}
