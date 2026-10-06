package com.caribexperience.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Tabla catalogo de estados de una reserva (PENDIENTE, CONFIRMADA, CANCELADA).
 */
@Entity
@Table(name = "estados_reserva")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EstadoReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String nombre;
}
