package com.caribexperience.repository;

import com.caribexperience.domain.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoReservaRepository extends JpaRepository<EstadoReserva, Long> {

    Optional<EstadoReserva> findByNombre(String nombre);
}
