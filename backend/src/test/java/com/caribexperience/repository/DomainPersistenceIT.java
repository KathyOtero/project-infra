package com.caribexperience.repository;

import com.caribexperience.common.EstadoExperienciaNombre;
import com.caribexperience.common.EstadoReservaNombre;
import com.caribexperience.common.RolNombre;
import com.caribexperience.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integracion (Etapa 2): valida que las entidades JPA mapean
 * exactamente contra el esquema creado por Flyway y que las relaciones
 * (Usuario -> Experiencia -> Reserva) se persisten y leen correctamente
 * contra una instancia real de MySQL (no un mock/H2).
 */
@Testcontainers
@SpringBootTest
class DomainPersistenceIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("caribexperience_test")
            .withUsername("test_user")
            .withPassword("test_pass");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private CiudadRepository ciudadRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private EstadoExperienciaRepository estadoExperienciaRepository;
    @Autowired
    private EstadoReservaRepository estadoReservaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ExperienciaRepository experienciaRepository;
    @Autowired
    private ReservaRepository reservaRepository;

    @Test
    void deberiaPersistirYRelacionarUsuarioExperienciaYReserva() {
        Rol rolGuia = rolRepository.findByNombre(RolNombre.GUIA).orElseThrow();
        Rol rolViajero = rolRepository.findByNombre(RolNombre.VIAJERO).orElseThrow();
        Ciudad cartagena = ciudadRepository.findAll().stream()
                .filter(c -> c.getNombre().equals("Cartagena")).findFirst().orElseThrow();
        Categoria nautica = categoriaRepository.findAll().stream()
                .filter(c -> c.getNombre().equals("Nautica")).findFirst().orElseThrow();
        EstadoExperiencia activa = estadoExperienciaRepository.findByNombre(EstadoExperienciaNombre.ACTIVA).orElseThrow();
        EstadoReserva confirmada = estadoReservaRepository.findByNombre(EstadoReservaNombre.CONFIRMADA).orElseThrow();

        Usuario guia = usuarioRepository.save(Usuario.builder()
                .nombre("Carlos").apellido("Perez").email("guia1@caribexperience.co")
                .passwordHash("hash-fake").rol(rolGuia).activo(true).build());

        Usuario viajero = usuarioRepository.save(Usuario.builder()
                .nombre("Ana").apellido("Gomez").email("viajero1@caribexperience.co")
                .passwordHash("hash-fake").rol(rolViajero).activo(true).build());

        Experiencia experiencia = experienciaRepository.save(Experiencia.builder()
                .guia(guia).ciudad(cartagena).categoria(nautica).estado(activa)
                .titulo("Tour en velero por las Islas del Rosario")
                .descripcion("Recorrido nautico de un dia completo con almuerzo tipico incluido")
                .precio(new BigDecimal("250000.00"))
                .cupoMax(10).cupoDisponible(10)
                .fecha(LocalDate.now().plusDays(7))
                .hora(LocalTime.of(8, 0))
                .fotoUrl("https://s3.amazonaws.com/caribexperience/tour-velero.jpg")
                .build());

        Reserva reserva = reservaRepository.save(Reserva.builder()
                .experiencia(experiencia).viajero(viajero).estado(confirmada)
                .cantidadPersonas(2)
                .fechaReserva(LocalDateTime.now())
                .build());

        assertThat(guia.getId()).isNotNull();
        assertThat(guia.getCreatedAt()).isNotNull();
        assertThat(experiencia.getId()).isNotNull();
        assertThat(reserva.getId()).isNotNull();

        long personasConfirmadas = reservaRepository.sumPersonasConfirmadasPorExperiencia(experiencia.getId());
        assertThat(personasConfirmadas).isEqualTo(2);

        assertThat(experienciaRepository.findByGuiaId(guia.getId(), org.springframework.data.domain.Pageable.unpaged())
                .getContent()).containsExactly(experiencia);

        assertThat(reservaRepository.findByViajeroId(viajero.getId(), org.springframework.data.domain.Pageable.unpaged())
                .getContent()).containsExactly(reserva);
    }
}
