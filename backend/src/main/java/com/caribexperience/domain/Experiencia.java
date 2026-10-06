package com.caribexperience.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Experiencia turistica publicada por un Guia. El cupo_disponible se
 * mantiene desnormalizado (en vez de calcularlo siempre por SUM sobre
 * reservas) por rendimiento de lectura en el listado publico; su
 * consistencia se garantiza en la capa de servicio dentro de una
 * transaccion al crear/cancelar una reserva (ver Etapa 3).
 */
@Entity
@Table(name = "experiencias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Experiencia extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guia_id", nullable = false)
    private Usuario guia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciudad_id", nullable = false)
    private Ciudad ciudad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoExperiencia estado;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "cupo_max", nullable = false)
    private Integer cupoMax;

    @Column(name = "cupo_disponible", nullable = false)
    private Integer cupoDisponible;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;
}
