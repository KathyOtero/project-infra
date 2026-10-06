package com.caribexperience.service;

import com.caribexperience.common.EstadoExperienciaNombre;
import com.caribexperience.domain.*;
import com.caribexperience.exception.OperacionNoAutorizadaException;
import com.caribexperience.exception.RecursoNoEncontradoException;
import com.caribexperience.repository.*;
import com.caribexperience.service.impl.ExperienciaServiceImpl;
import com.caribexperience.web.dto.experiencia.ExperienciaRequest;
import com.caribexperience.web.dto.experiencia.ExperienciaResponse;
import com.caribexperience.web.mapper.ExperienciaMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de ExperienciaServiceImpl. Cubren las reglas de negocio
 * mas importantes de esta capa: validacion de rol GUIA al crear, y
 * validacion de propiedad (ownership) al actualizar/cancelar -- ningun guia
 * puede modificar la experiencia de otro.
 *
 * NOTA: @PreAuthorize no se ejecuta en un test unitario puro con Mockito
 * (requiere el proxy de Spring AOP alrededor del bean real). La cobertura
 * de la autorizacion por rol a nivel de framework se hace en
 * SecurityIntegrationTest (@SpringBootTest), que si carga el contexto
 * completo de Spring Security.
 */
@ExtendWith(MockitoExtension.class)
class ExperienciaServiceImplTest {

    @Mock
    private ExperienciaRepository experienciaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CiudadRepository ciudadRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private EstadoExperienciaRepository estadoExperienciaRepository;
    @Mock
    private ExperienciaMapper experienciaMapper;

    @InjectMocks
    private ExperienciaServiceImpl experienciaService;

    private Usuario guia;
    private Usuario viajero;
    private Ciudad ciudad;
    private Categoria categoria;
    private EstadoExperiencia estadoActiva;
    private ExperienciaRequest request;

    @BeforeEach
    void setUp() {
        Rol rolGuia = Rol.builder().id(1L).nombre("GUIA").build();
        Rol rolViajero = Rol.builder().id(2L).nombre("VIAJERO").build();

        guia = Usuario.builder().id(10L).nombre("Carlos").apellido("Perez")
                .email("guia1@test.co").passwordHash("hash").rol(rolGuia).activo(true).build();
        viajero = Usuario.builder().id(20L).nombre("Ana").apellido("Gomez")
                .email("viajero1@test.co").passwordHash("hash").rol(rolViajero).activo(true).build();

        ciudad = Ciudad.builder().id(1L).nombre("Cartagena").departamento("Bolivar").build();
        categoria = Categoria.builder().id(1L).nombre("Aventura").build();
        estadoActiva = EstadoExperiencia.builder().id(1L).nombre(EstadoExperienciaNombre.ACTIVA).build();

        request = new ExperienciaRequest(
                "Tour Cartagena", "Recorrido por la ciudad amurallada",
                new BigDecimal("50.00"), 3, LocalDate.now().plusDays(5), LocalTime.of(10, 0),
                null, 1L, 1L);
    }

    @Test
    void crear_deberiaCrearExperienciaConCupoDisponibleIgualACupoMax_cuandoElUsuarioEsGuia() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(guia));
        when(ciudadRepository.findById(1L)).thenReturn(Optional.of(ciudad));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(estadoExperienciaRepository.findByNombre(EstadoExperienciaNombre.ACTIVA)).thenReturn(Optional.of(estadoActiva));
        when(experienciaRepository.save(any(Experiencia.class))).thenAnswer(inv -> inv.getArgument(0));
        when(experienciaMapper.toResponse(any(Experiencia.class))).thenAnswer(inv -> {
            Experiencia e = inv.getArgument(0);
            return new ExperienciaResponse(null, e.getGuia().getId(), null, 1L, "Cartagena", 1L, "Aventura",
                    "ACTIVA", e.getTitulo(), e.getDescripcion(), e.getPrecio(), e.getCupoMax(),
                    e.getCupoDisponible(), e.getFecha(), e.getHora(), null, null);
        });

        ExperienciaResponse response = experienciaService.crear(10L, request);

        assertThat(response.cupoDisponible()).isEqualTo(3);
        assertThat(response.cupoMax()).isEqualTo(3);

        ArgumentCaptor<Experiencia> captor = ArgumentCaptor.forClass(Experiencia.class);
        verify(experienciaRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(estadoActiva);
    }

    @Test
    void crear_deberiaLanzarOperacionNoAutorizada_cuandoElUsuarioNoEsGuia() {
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(viajero));

        assertThatThrownBy(() -> experienciaService.crear(20L, request))
                .isInstanceOf(OperacionNoAutorizadaException.class)
                .hasMessageContaining("GUIA");
    }

    @Test
    void crear_deberiaLanzarRecursoNoEncontrado_cuandoLaCiudadNoExiste() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(guia));
        when(ciudadRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> experienciaService.crear(10L, request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Ciudad");
    }

    @Test
    void actualizar_deberiaLanzarOperacionNoAutorizada_cuandoElGuiaNoEsElDuenoDeLaExperiencia() {
        Experiencia experienciaAjena = Experiencia.builder()
                .id(100L).guia(guia).ciudad(ciudad).categoria(categoria).estado(estadoActiva)
                .titulo("Tour de otro guia").descripcion("desc")
                .precio(new BigDecimal("30.00")).cupoMax(5).cupoDisponible(5)
                .fecha(LocalDate.now().plusDays(2)).hora(LocalTime.of(9, 0)).build();

        when(experienciaRepository.findById(100L)).thenReturn(Optional.of(experienciaAjena));

        // Otro guia (id 99, no el dueno id 10) intenta actualizar.
        assertThatThrownBy(() -> experienciaService.actualizar(100L, 99L, request))
                .isInstanceOf(OperacionNoAutorizadaException.class)
                .hasMessageContaining("permiso");

        verify(experienciaRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void actualizar_deberiaAjustarCupoDisponibleProporcionalmente_cuandoSeAmpliaElCupoMax() {
        // Experiencia con 2 cupos ya ocupados (cupoMax=5, cupoDisponible=3).
        Experiencia experienciaExistente = Experiencia.builder()
                .id(100L).guia(guia).ciudad(ciudad).categoria(categoria).estado(estadoActiva)
                .titulo("Tour").descripcion("desc")
                .precio(new BigDecimal("30.00")).cupoMax(5).cupoDisponible(3)
                .fecha(LocalDate.now().plusDays(2)).hora(LocalTime.of(9, 0)).build();

        ExperienciaRequest requestAmpliado = new ExperienciaRequest(
                "Tour actualizado", "desc actualizada", new BigDecimal("35.00"), 8,
                LocalDate.now().plusDays(3), LocalTime.of(11, 0), null, 1L, 1L);

        when(experienciaRepository.findById(100L)).thenReturn(Optional.of(experienciaExistente));
        when(ciudadRepository.findById(1L)).thenReturn(Optional.of(ciudad));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(experienciaRepository.save(any(Experiencia.class))).thenAnswer(inv -> inv.getArgument(0));
        when(experienciaMapper.toResponse(any(Experiencia.class))).thenAnswer(inv -> {
            Experiencia e = inv.getArgument(0);
            return new ExperienciaResponse(e.getId(), e.getGuia().getId(), null, 1L, "Cartagena", 1L, "Aventura",
                    "ACTIVA", e.getTitulo(), e.getDescripcion(), e.getPrecio(), e.getCupoMax(),
                    e.getCupoDisponible(), e.getFecha(), e.getHora(), null, null);
        });

        ExperienciaResponse response = experienciaService.actualizar(100L, 10L, requestAmpliado);

        // 2 cupos ya ocupados (5-3) se preservan: nuevo cupoDisponible = 8 - 2 = 6.
        assertThat(response.cupoMax()).isEqualTo(8);
        assertThat(response.cupoDisponible()).isEqualTo(6);
    }

    @Test
    void cancelar_deberiaLanzarOperacionNoAutorizada_cuandoElGuiaNoEsElDueno() {
        Experiencia experienciaAjena = Experiencia.builder()
                .id(100L).guia(guia).ciudad(ciudad).categoria(categoria).estado(estadoActiva)
                .titulo("Tour").descripcion("desc")
                .precio(new BigDecimal("30.00")).cupoMax(5).cupoDisponible(5)
                .fecha(LocalDate.now().plusDays(2)).hora(LocalTime.of(9, 0)).build();

        when(experienciaRepository.findById(100L)).thenReturn(Optional.of(experienciaAjena));

        assertThatThrownBy(() -> experienciaService.cancelar(100L, 99L))
                .isInstanceOf(OperacionNoAutorizadaException.class);
    }
}
