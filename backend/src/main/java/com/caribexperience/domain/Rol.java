package com.caribexperience.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Tabla catalogo de roles de usuario (GUIA, VIAJERO, ADMIN).
 * Modelada como tabla en vez de enum embebido para poder agregar roles
 * nuevos sin necesidad de una migracion de esquema.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String nombre;
}
