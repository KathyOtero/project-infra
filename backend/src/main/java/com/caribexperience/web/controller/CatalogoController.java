package com.caribexperience.web.controller;

import com.caribexperience.repository.CategoriaRepository;
import com.caribexperience.repository.CiudadRepository;
import com.caribexperience.web.dto.catalogo.CatalogoItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone los catalogos de solo lectura (ciudades, categorias) que el
 * frontend necesita para poblar selects/combos al crear una experiencia.
 * Se accede directo al repositorio (sin capa de servicio) porque no hay
 * ninguna regla de negocio de por medio: es una simple proyeccion de datos.
 */
@RestController
@RequestMapping("/api/catalogos")
@RequiredArgsConstructor
public class CatalogoController {

    private final CiudadRepository ciudadRepository;
    private final CategoriaRepository categoriaRepository;

    @GetMapping("/ciudades")
    public List<CatalogoItemResponse> ciudades() {
        return ciudadRepository.findAll().stream()
                .map(c -> new CatalogoItemResponse(c.getId(), c.getNombre() + " - " + c.getDepartamento()))
                .toList();
    }

    @GetMapping("/categorias")
    public List<CatalogoItemResponse> categorias() {
        return categoriaRepository.findAll().stream()
                .map(c -> new CatalogoItemResponse(c.getId(), c.getNombre()))
                .toList();
    }
}
