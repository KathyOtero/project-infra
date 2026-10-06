package com.caribexperience.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Usuario de la plataforma. Un usuario tiene un unico rol (GUIA o VIAJERO)
 * que determina que puede hacer en el sistema (ver reglas en la capa de
 * servicio y seguridad).
 *
 * No se modela la relacion inversa (@OneToMany hacia Experiencia/Reserva)
 * a proposito: mantener las relaciones unidireccionales desde el lado
 * "muchos" evita colecciones perezosas costosas y ciclos de serializacion
 * JSON. Las consultas de "mis experiencias" / "mis reservas" se resuelven
 * con metodos de repositorio (ver ExperienciaRepository/ReservaRepository).
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Usuario extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
