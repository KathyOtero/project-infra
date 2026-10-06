package com.caribexperience.web.controller;

import com.caribexperience.service.interfaces.UsuarioService;
import com.caribexperience.web.dto.usuario.UsuarioRegistroRequest;
import com.caribexperience.web.dto.usuario.UsuarioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * NOTA: en esta etapa no existe todavia autenticacion (Etapa 5 - JWT), por
 * lo que el registro es publico y no requiere token. El login llegara en
 * la Etapa 5 junto con la emision de JWT.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody UsuarioRegistroRequest request) {
        UsuarioResponse response = usuarioService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }
}
