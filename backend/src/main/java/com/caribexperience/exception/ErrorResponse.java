package com.caribexperience.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Estructura uniforme de error para toda la API. `errores` solo se llena
 * cuando el error viene de validacion de campos (@Valid); en el resto de
 * los casos queda null para no ensuciar la respuesta.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> errores
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, null);
    }

    public static ErrorResponse deValidacion(int status, String error, String message, String path,
                                              Map<String, String> errores) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, errores);
    }
}
