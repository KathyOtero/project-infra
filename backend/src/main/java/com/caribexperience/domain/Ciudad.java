package com.caribexperience.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Tabla catalogo de ciudades de la Costa Caribe donde se ofrecen experiencias.
 */
@Entity
@Table(name = "ciudades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Ciudad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String departamento;
}
