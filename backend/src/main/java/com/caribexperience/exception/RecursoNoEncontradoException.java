package com.caribexperience.exception;

/**
 * Se lanza cuando no se encuentra una entidad por su identificador.
 * Se traduce a HTTP 404 en el manejador global (Etapa 4).
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
