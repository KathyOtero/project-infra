package com.caribexperience.common;

/**
 * Nombres de rol tal como existen en la tabla catalogo `roles` (V2 seed data).
 * Centralizar estas constantes evita "magic strings" repetidos en servicios,
 * seguridad y specs de consulta.
 */
public final class RolNombre {

    public static final String GUIA = "GUIA";
    public static final String VIAJERO = "VIAJERO";
    public static final String ADMIN = "ADMIN";

    private RolNombre() {
    }
}
