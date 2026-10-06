package com.caribexperience.exception;

public class CupoInsuficienteException extends ReglaDeNegocioException {

    public CupoInsuficienteException(int solicitado, int disponible) {
        super("Cupo insuficiente: se solicitaron " + solicitado + " puesto(s) pero solo hay "
                + disponible + " disponible(s)");
    }
}
