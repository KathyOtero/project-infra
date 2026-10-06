package com.caribexperience.web.dto.auth;

/**
 * Respuesta de login: el token JWT y algunos datos basicos del usuario para
 * que el frontend no necesite decodificar el token solo para mostrar el
 * nombre/rol en la UI.
 */
public record LoginResponse(
        String token,
        String tipo,
        Long usuarioId,
        String nombre,
        String email,
        String rol
) {
    public static LoginResponse de(String token, Long usuarioId, String nombre, String email, String rol) {
        return new LoginResponse(token, "Bearer", usuarioId, nombre, email, rol);
    }
}
