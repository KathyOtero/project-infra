package com.caribexperience.repository;

import com.caribexperience.domain.EstadoExperiencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoExperienciaRepository extends JpaRepository<EstadoExperiencia, Long> {

    Optional<EstadoExperiencia> findByNombre(String nombre);
}
