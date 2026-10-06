package com.caribexperience.web.controller;

import com.caribexperience.security.UsuarioPrincipal;
import com.caribexperience.service.interfaces.ExperienciaService;
import com.caribexperience.web.dto.common.PaginaResponse;
import com.caribexperience.web.dto.experiencia.ExperienciaFiltro;
import com.caribexperience.web.dto.experiencia.ExperienciaRequest;
import com.caribexperience.web.dto.experiencia.ExperienciaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Desde la Etapa 5, el guia autor de la operacion ya NO se recibe como query
 * param (ese diseno de la Etapa 4 era inseguro: cualquiera podia suplantar a
 * otro guia). Ahora se obtiene del JWT validado por JwtAuthenticationFilter
 * via @AuthenticationPrincipal; es imposible operar en nombre de otro
 * usuario sin su token.
 */
@RestController
@RequestMapping("/api/experiencias")
@RequiredArgsConstructor
public class ExperienciaController {

    private final ExperienciaService experienciaService;

    @PostMapping
    public ResponseEntity<ExperienciaResponse> crear(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                       @Valid @RequestBody ExperienciaRequest request) {
        ExperienciaResponse response = experienciaService.crear(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExperienciaResponse> actualizar(@PathVariable Long id,
                                                            @AuthenticationPrincipal UsuarioPrincipal principal,
                                                            @Valid @RequestBody ExperienciaRequest request) {
        return ResponseEntity.ok(experienciaService.actualizar(id, principal.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id,
                                          @AuthenticationPrincipal UsuarioPrincipal principal) {
        experienciaService.cancelar(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /** Publico: no requiere autenticacion (ver SecurityConfig). */
    @GetMapping("/{id}")
    public ResponseEntity<ExperienciaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(experienciaService.obtenerPorId(id));
    }

    /** Listado publico con filtros opcionales por ciudad, categoria y rango de fechas. */
    @GetMapping
    public ResponseEntity<PaginaResponse<ExperienciaResponse>> buscar(
            @RequestParam(required = false) Long ciudadId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Pageable pageable) {
        ExperienciaFiltro filtro = new ExperienciaFiltro(ciudadId, categoriaId, fechaDesde, fechaHasta);
        return ResponseEntity.ok(PaginaResponse.de(experienciaService.buscar(filtro, pageable)));
    }

    /** "Mis experiencias" del guia autenticado. */
    @GetMapping("/mias")
    public ResponseEntity<PaginaResponse<ExperienciaResponse>> misExperiencias(
            @AuthenticationPrincipal UsuarioPrincipal principal, Pageable pageable) {
        return ResponseEntity.ok(PaginaResponse.de(experienciaService.listarPorGuia(principal.getId(), pageable)));
    }
}
