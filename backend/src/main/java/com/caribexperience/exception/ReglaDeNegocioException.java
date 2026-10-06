package com.caribexperience.exception;

/**
 * Excepcion base para violaciones de reglas de negocio (no de validacion de
 * formato, sino de logica del dominio: cupos, estados invalidos, duplicados, etc.).
 * Se traduce a HTTP 409/400 en el manejador global (Etapa 4).
 */
public class ReglaDeNegocioException extends RuntimeException {

    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
