package com.caribexperience.web.controller;

import com.caribexperience.security.UsuarioPrincipal;
import com.caribexperience.service.interfaces.ReservaService;
import com.caribexperience.web.dto.common.PaginaResponse;
import com.caribexperience.web.dto.reserva.ReservaRequest;
import com.caribexperience.web.dto.reserva.ReservaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Desde la Etapa 5, `viajeroId`/`guiaId` ya no llegan como query param: se
 * obtienen del usuario autenticado (JWT) via @AuthenticationPrincipal.
 */
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    public ResponseEntity<ReservaResponse> reservar(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                      @Valid @RequestBody ReservaRequest request) {
        ReservaResponse response = reservaService.reservar(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id,
                                          @AuthenticationPrincipal UsuarioPrincipal principal) {
        reservaService.cancelar(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /** Historial de reservas del viajero autenticado. */
    @GetMapping("/mias")
    public ResponseEntity<PaginaResponse<ReservaResponse>> misReservas(
            @AuthenticationPrincipal UsuarioPrincipal principal, Pageable pageable) {
        return ResponseEntity.ok(PaginaResponse.de(reservaService.misReservas(principal.getId(), pageable)));
    }

    /** Reservas recibidas en las experiencias publicadas por el guia autenticado. */
    @GetMapping("/recibidas")
    public ResponseEntity<PaginaResponse<ReservaResponse>> reservasRecibidas(
            @AuthenticationPrincipal UsuarioPrincipal principal, Pageable pageable) {
        return ResponseEntity.ok(PaginaResponse.de(reservaService.reservasRecibidas(principal.getId(), pageable)));
    }
}
