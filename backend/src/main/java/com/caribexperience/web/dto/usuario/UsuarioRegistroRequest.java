package com.caribexperience.web.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para registrar un usuario nuevo (Guia o Viajero).
 */
public record UsuarioRegistroRequest(

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        String apellido,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, message = "La contrasena debe tener minimo 8 caracteres")
        String password,

        @NotBlank(message = "El rol es obligatorio")
        @Pattern(regexp = "GUIA|VIAJERO", message = "El rol debe ser GUIA o VIAJERO")
        String rol
) {
}
