package com.caribexperience.exception;

/**
 * Se lanza cuando un usuario intenta operar sobre un recurso que no le
 * pertenece (ej. editar la experiencia de otro guia) o con un rol
 * incorrecto para la operacion. Se traduce a HTTP 403 (Etapa 4).
 */
public class OperacionNoAutorizadaException extends RuntimeException {

    public OperacionNoAutorizadaException(String mensaje) {
        super(mensaje);
    }
}
