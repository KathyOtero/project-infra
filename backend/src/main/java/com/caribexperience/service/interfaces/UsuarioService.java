package com.caribexperience.service.interfaces;

import com.caribexperience.web.dto.usuario.UsuarioRegistroRequest;
import com.caribexperience.web.dto.usuario.UsuarioResponse;

public interface UsuarioService {

    /**
     * Registra un nuevo usuario (Guia o Viajero). Valida que el email no
     * este ya registrado y almacena la contrasena hasheada (nunca en texto plano).
     */
    UsuarioResponse registrar(UsuarioRegistroRequest request);

    UsuarioResponse obtenerPorId(Long id);
}
