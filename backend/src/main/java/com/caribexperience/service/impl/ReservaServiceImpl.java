package com.caribexperience.service.impl;

import com.caribexperience.common.EstadoReservaNombre;
import com.caribexperience.common.RolNombre;
import com.caribexperience.domain.EstadoReserva;
import com.caribexperience.domain.Experiencia;
import com.caribexperience.domain.Reserva;
import com.caribexperience.domain.Usuario;
import com.caribexperience.exception.CupoInsuficienteException;
import com.caribexperience.exception.OperacionNoAutorizadaException;
import com.caribexperience.exception.RecursoNoEncontradoException;
import com.caribexperience.repository.EstadoReservaRepository;
import com.caribexperience.repository.ExperienciaRepository;
import com.caribexperience.repository.ReservaRepository;
import com.caribexperience.repository.UsuarioRepository;
import com.caribexperience.service.interfaces.ReservaService;
import com.caribexperience.web.dto.reserva.ReservaRequest;
import com.caribexperience.web.dto.reserva.ReservaResponse;
import com.caribexperience.web.mapper.ReservaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final ExperienciaRepository experienciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstadoReservaRepository estadoReservaRepository;
    private final ReservaMapper reservaMapper;

    @Override
    @Transactional
    @PreAuthorize("hasRole('VIAJERO')")
    public ReservaResponse reservar(Long viajeroId, ReservaRequest request) {
        Usuario viajero = obtenerViajeroValidado(viajeroId);

        // findByIdParaActualizar usa SELECT ... FOR UPDATE: bloquea la fila de
        // la experiencia hasta que esta transaccion termine, evitando que dos
        // reservas concurrentes lean el mismo cupo_disponible y lo sobrevendan.
        Experiencia experiencia = experienciaRepository.findByIdParaActualizar(request.experienciaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Experiencia no encontrada con id: " + request.experienciaId()));

        if (experiencia.getCupoDisponible() < request.cantidadPersonas()) {
            throw new CupoInsuficienteException(request.cantidadPersonas(), experiencia.getCupoDisponible());
        }

        experiencia.setCupoDisponible(experiencia.getCupoDisponible() - request.cantidadPersonas());
        experienciaRepository.save(experiencia);

        EstadoReserva confirmada = obtenerEstado(EstadoReservaNombre.CONFIRMADA);

        Reserva reserva = Reserva.builder()
                .experiencia(experiencia)
                .viajero(viajero)
                .estado(confirmada)
                .cantidadPersonas(request.cantidadPersonas())
                .fechaReserva(LocalDateTime.now())
                .build();

        return reservaMapper.toResponse(reservaRepository.save(reserva));
    }

    @Override
    @Transactional
    public void cancelar(Long reservaId, Long viajeroId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada con id: " + reservaId));

        if (!reserva.getViajero().getId().equals(viajeroId)) {
            throw new OperacionNoAutorizadaException("No tienes permiso para cancelar esta reserva");
        }

        if (EstadoReservaNombre.CANCELADA.equals(reserva.getEstado().getNombre())) {
            return; // idempotente: cancelar una reserva ya cancelada no hace nada
        }

        // Se recupera la experiencia con bloqueo pesimista por la misma razon
        // que en reservar(): esta operacion tambien modifica el cupo compartido.
        Experiencia experiencia = experienciaRepository.findByIdParaActualizar(reserva.getExperiencia().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Experiencia asociada no encontrada"));

        experiencia.setCupoDisponible(experiencia.getCupoDisponible() + reserva.getCantidadPersonas());
        experienciaRepository.save(experiencia);

        reserva.setEstado(obtenerEstado(EstadoReservaNombre.CANCELADA));
        reservaRepository.save(reserva);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservaResponse> misReservas(Long viajeroId, Pageable pageable) {
        return reservaRepository.findByViajeroId(viajeroId, pageable).map(reservaMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservaResponse> reservasRecibidas(Long guiaId, Pageable pageable) {
        return reservaRepository.findByExperienciaGuiaId(guiaId, pageable).map(reservaMapper::toResponse);
    }

    // ---- Helpers privados ----

    private Usuario obtenerViajeroValidado(Long viajeroId) {
        Usuario viajero = usuarioRepository.findById(viajeroId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + viajeroId));
        if (!RolNombre.VIAJERO.equals(viajero.getRol().getNombre())) {
            throw new OperacionNoAutorizadaException("Solo un usuario con rol VIAJERO puede reservar experiencias");
        }
        return viajero;
    }

    private EstadoReserva obtenerEstado(String nombre) {
        return estadoReservaRepository.findByNombre(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado de reserva no encontrado: " + nombre));
    }
}
