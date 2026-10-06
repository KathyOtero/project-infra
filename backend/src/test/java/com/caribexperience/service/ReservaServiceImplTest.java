package com.caribexperience.service;

import com.caribexperience.common.EstadoReservaNombre;
import com.caribexperience.domain.*;
import com.caribexperience.exception.CupoInsuficienteException;
import com.caribexperience.exception.OperacionNoAutorizadaException;
import com.caribexperience.exception.RecursoNoEncontradoException;
import com.caribexperience.repository.EstadoReservaRepository;
import com.caribexperience.repository.ExperienciaRepository;
import com.caribexperience.repository.ReservaRepository;
import com.caribexperience.repository.UsuarioRepository;
import com.caribexperience.service.impl.ReservaServiceImpl;
import com.caribexperience.web.dto.reserva.ReservaRequest;
import com.caribexperience.web.dto.reserva.ReservaResponse;
import com.caribexperience.web.mapper.ReservaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de ReservaServiceImpl -- la logica de negocio mas
 * critica del proyecto (control de cupo). Se prueba exhaustivamente que el
 * cupo se descuenta y restaura correctamente y que nunca se permite
 * sobrevender una experiencia.
 */
@ExtendWith(MockitoExtension.class)
class ReservaServiceImplTest {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ExperienciaRepository experienciaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EstadoReservaRepository estadoReservaRepository;
    @Mock
    private ReservaMapper reservaMapper;

    @InjectMocks
    private ReservaServiceImpl reservaService;

    private Usuario viajero;
    private Usuario guia;
    private Experiencia experiencia;
    private EstadoReserva estadoConfirmada;
    private EstadoReserva estadoCancelada;

    @BeforeEach
    void setUp() {
        Rol rolViajero = Rol.builder().id(2L).nombre("VIAJERO").build();
        Rol rolGuia = Rol.builder().id(1L).nombre("GUIA").build();

        viajero = Usuario.builder().id(20L).nombre("Ana").apellido("Gomez")
                .email("viajero1@test.co").passwordHash("hash").rol(rolViajero).activo(true).build();
        guia = Usuario.builder().id(10L).nombre("Carlos").apellido("Perez")
                .email("guia1@test.co").passwordHash("hash").rol(rolGuia).activo(true).build();

        Ciudad ciudad = Ciudad.builder().id(1L).nombre("Cartagena").departamento("Bolivar").build();
        Categoria categoria = Categoria.builder().id(1L).nombre("Aventura").build();
        EstadoExperiencia estadoActiva = EstadoExperiencia.builder().id(1L).nombre("ACTIVA").build();

        experiencia = Experiencia.builder()
                .id(100L).guia(guia).ciudad(ciudad).categoria(categoria).estado(estadoActiva)
                .titulo("Tour Cartagena").descripcion("desc")
                .precio(new BigDecimal("50.00")).cupoMax(3).cupoDisponible(3)
                .fecha(LocalDate.now().plusDays(5)).hora(LocalTime.of(10, 0)).build();

        estadoConfirmada = EstadoReserva.builder().id(1L).nombre(EstadoReservaNombre.CONFIRMADA).build();
        estadoCancelada = EstadoReserva.builder().id(2L).nombre(EstadoReservaNombre.CANCELADA).build();
    }

    @Test
    void reservar_deberiaDescontarElCupoDisponible_cuandoHaySuficienteCupo() {
        ReservaRequest request = new ReservaRequest(100L, 2);

        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(viajero));
        when(experienciaRepository.findByIdParaActualizar(100L)).thenReturn(Optional.of(experiencia));
        when(estadoReservaRepository.findByNombre(EstadoReservaNombre.CONFIRMADA)).thenReturn(Optional.of(estadoConfirmada));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaMapper.toResponse(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            return new ReservaResponse(null, r.getExperiencia().getId(), r.getExperiencia().getTitulo(),
                    r.getViajero().getId(), null, r.getCantidadPersonas(), r.getEstado().getNombre(), r.getFechaReserva());
        });

        ReservaResponse response = reservaService.reservar(20L, request);

        assertThat(response.cantidadPersonas()).isEqualTo(2);
        assertThat(response.estado()).isEqualTo("CONFIRMADA");

        // El cupo disponible de la experiencia debe quedar en 3 - 2 = 1.
        ArgumentCaptor<Experiencia> captor = ArgumentCaptor.forClass(Experiencia.class);
        verify(experienciaRepository).save(captor.capture());
        assertThat(captor.getValue().getCupoDisponible()).isEqualTo(1);
    }

    @Test
    void reservar_deberiaLanzarCupoInsuficiente_cuandoSeSolicitanMasPersonasQueElCupoDisponible() {
        ReservaRequest request = new ReservaRequest(100L, 5); // solo hay 3 disponibles

        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(viajero));
        when(experienciaRepository.findByIdParaActualizar(100L)).thenReturn(Optional.of(experiencia));

        assertThatThrownBy(() -> reservaService.reservar(20L, request))
                .isInstanceOf(CupoInsuficienteException.class)
                .hasMessageContaining("5")
                .hasMessageContaining("3");

        // Ante cupo insuficiente, la experiencia NUNCA debe guardarse modificada.
        verify(experienciaRepository, never()).save(any());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void reservar_deberiaLanzarOperacionNoAutorizada_cuandoElUsuarioNoEsViajero() {
        ReservaRequest request = new ReservaRequest(100L, 2);

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(guia)); // guia intentando reservar

        assertThatThrownBy(() -> reservaService.reservar(10L, request))
                .isInstanceOf(OperacionNoAutorizadaException.class)
                .hasMessageContaining("VIAJERO");
    }

    @Test
    void reservar_deberiaLanzarRecursoNoEncontrado_cuandoLaExperienciaNoExiste() {
        ReservaRequest request = new ReservaRequest(999L, 2);

        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(viajero));
        when(experienciaRepository.findByIdParaActualizar(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.reservar(20L, request))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void cancelar_deberiaRestaurarElCupoDisponible_cuandoElViajeroEsElDueno() {
        Reserva reservaExistente = Reserva.builder()
                .id(1L).experiencia(experiencia).viajero(viajero).estado(estadoConfirmada)
                .cantidadPersonas(2).fechaReserva(java.time.LocalDateTime.now()).build();

        experiencia.setCupoDisponible(1); // ya se habian descontado 2 cupos previamente

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reservaExistente));
        when(experienciaRepository.findByIdParaActualizar(100L)).thenReturn(Optional.of(experiencia));
        when(estadoReservaRepository.findByNombre(EstadoReservaNombre.CANCELADA)).thenReturn(Optional.of(estadoCancelada));

        reservaService.cancelar(1L, 20L);

        ArgumentCaptor<Experiencia> captor = ArgumentCaptor.forClass(Experiencia.class);
        verify(experienciaRepository).save(captor.capture());
        // Cupo restaurado: 1 (disponible) + 2 (de la reserva cancelada) = 3.
        assertThat(captor.getValue().getCupoDisponible()).isEqualTo(3);

        ArgumentCaptor<Reserva> reservaCaptor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(reservaCaptor.capture());
        assertThat(reservaCaptor.getValue().getEstado().getNombre()).isEqualTo(EstadoReservaNombre.CANCELADA);
    }

    @Test
    void cancelar_deberiaLanzarOperacionNoAutorizada_cuandoElViajeroNoEsElDueno() {
        Reserva reservaDeOtroViajero = Reserva.builder()
                .id(1L).experiencia(experiencia).viajero(viajero).estado(estadoConfirmada)
                .cantidadPersonas(2).fechaReserva(java.time.LocalDateTime.now()).build();

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reservaDeOtroViajero));

        // Otro viajero (id 999, no el dueno id 20) intenta cancelar.
        assertThatThrownBy(() -> reservaService.cancelar(1L, 999L))
                .isInstanceOf(OperacionNoAutorizadaException.class)
                .hasMessageContaining("permiso");

        verify(experienciaRepository, never()).save(any());
    }

    @Test
    void cancelar_deberiaSerIdempotente_cuandoLaReservaYaEstaCancelada() {
        Reserva reservaYaCancelada = Reserva.builder()
                .id(1L).experiencia(experiencia).viajero(viajero).estado(estadoCancelada)
                .cantidadPersonas(2).fechaReserva(java.time.LocalDateTime.now()).build();

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reservaYaCancelada));

        reservaService.cancelar(1L, 20L);

        // No debe tocar el cupo ni volver a guardar la reserva.
        verify(experienciaRepository, never()).save(any());
        verify(reservaRepository, never()).save(any());
    }
}
