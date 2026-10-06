package com.caribexperience.service.interfaces;

import com.caribexperience.web.dto.reserva.ReservaRequest;
import com.caribexperience.web.dto.reserva.ReservaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReservaService {

    /**
     * Crea una reserva CONFIRMADA y descuenta el cupo de la experiencia de
     * forma atomica (bloqueo pesimista para evitar sobreventa concurrente).
     * Solo puede invocarlo un usuario con rol VIAJERO.
     */
    ReservaResponse reservar(Long viajeroId, ReservaRequest request);

    /**
     * Cancela una reserva propia y devuelve el cupo a la experiencia.
     * Valida que el viajero autenticado sea el dueno de la reserva.
     */
    void cancelar(Long reservaId, Long viajeroId);

    /** Historial de reservas hechas por un viajero ("mis reservas"). */
    Page<ReservaResponse> misReservas(Long viajeroId, Pageable pageable);

    /** Reservas recibidas en las experiencias publicadas por un guia. */
    Page<ReservaResponse> reservasRecibidas(Long guiaId, Pageable pageable);
}
