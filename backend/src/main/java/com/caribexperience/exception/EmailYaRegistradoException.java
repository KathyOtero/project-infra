package com.caribexperience.exception;

public class EmailYaRegistradoException extends ReglaDeNegocioException {

    public EmailYaRegistradoException(String email) {
        super("Ya existe un usuario registrado con el email: " + email);
    }
}
