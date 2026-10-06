package com.caribexperience.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Reserva realizada por un Viajero sobre una Experiencia.
 */
@Entity
@Table(name = "reservas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Reserva extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiencia_id", nullable = false)
    private Experiencia experiencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viajero_id", nullable = false)
    private Usuario viajero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoReserva estado;

    @Column(name = "cantidad_personas", nullable = false)
    private Integer cantidadPersonas;

    @Column(name = "fecha_reserva", nullable = false)
    private LocalDateTime fechaReserva;
}
