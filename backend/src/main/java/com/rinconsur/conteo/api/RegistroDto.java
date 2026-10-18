package com.rinconsur.conteo.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Registro de flujo: cuántas personas de un tipo y rango etario ingresaron por un acceso. */
public record RegistroDto(
        Long id,
        @NotNull @Min(1) @Max(4) Integer accesoId,
        @NotNull Long fechaHora,
        @NotBlank @Pattern(regexp = "TRANSITO|COMPRA", message = "tipo debe ser TRANSITO o COMPRA") String tipo,
        @NotBlank String rangoEtario,
        @NotNull @Min(1) @Max(1000) Integer cantidad
) {
    public RegistroDto conId(long nuevoId) {
        return new RegistroDto(nuevoId, accesoId, fechaHora, tipo, rangoEtario, cantidad);
    }
}
