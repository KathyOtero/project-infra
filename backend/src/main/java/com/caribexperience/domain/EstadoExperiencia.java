package com.caribexperience.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Tabla catalogo de estados del ciclo de vida de una experiencia
 * (ACTIVA, PAUSADA, FINALIZADA, CANCELADA).
 */
@Entity
@Table(name = "estados_experiencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EstadoExperiencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String nombre;
}
