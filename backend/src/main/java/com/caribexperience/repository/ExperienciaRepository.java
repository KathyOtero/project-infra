package com.caribexperience.repository;

import com.caribexperience.domain.Experiencia;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * JpaSpecificationExecutor permite construir busquedas dinamicas (filtros
 * opcionales por ciudad/categoria/fecha) sin explotar en decenas de metodos
 * derivados combinados. Las Specifications se arman en la capa de servicio
 * (Etapa 3), manteniendo el repositorio libre de logica de negocio.
 */
public interface ExperienciaRepository extends JpaRepository<Experiencia, Long>,
        JpaSpecificationExecutor<Experiencia> {

    Page<Experiencia> findByGuiaId(Long guiaId, Pageable pageable);

    /**
     * Bloqueo pesimista de escritura (SELECT ... FOR UPDATE). Se usa al
     * confirmar una reserva para evitar que dos peticiones concurrentes
     * lean el mismo cupo_disponible y ambas lo descuenten, sobrevendiendo
     * la experiencia (condicion de carrera clasica en sistemas de reservas).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Experiencia e where e.id = :id")
    Optional<Experiencia> findByIdParaActualizar(@Param("id") Long id);
}
