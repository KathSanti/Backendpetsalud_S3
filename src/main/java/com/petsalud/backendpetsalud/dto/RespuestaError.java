package com.petsalud.backendpetsalud.dto;

import java.time.LocalDateTime;

public record RespuestaError(
        int status,
        String mensaje,
        String ruta,
        LocalDateTime fecha
) {
}