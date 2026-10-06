package com.caribexperience.web.dto.usuario;

import java.time.LocalDateTime;

/**
 * Datos de un usuario expuestos por la API. Nunca incluye password_hash.
 */
public record UsuarioResponse(
        Long id,
        String nombre,
        String apellido,
        String email,
        String rol,
        Boolean activo,
        LocalDateTime createdAt
) {
}
