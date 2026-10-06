package com.caribexperience.repository;

import com.caribexperience.domain.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    Page<Reserva> findByViajeroId(Long viajeroId, Pageable pageable);

    Page<Reserva> findByExperienciaId(Long experienciaId, Pageable pageable);

    /** Reservas recibidas por un guia: todas las reservas de sus experiencias. */
    Page<Reserva> findByExperienciaGuiaId(Long guiaId, Pageable pageable);

    /**
     * Suma de personas de reservas CONFIRMADAS para una experiencia. Sirve
     * como verificacion de consistencia en la capa de servicio, ya que el
     * cupo_disponible se mantiene desnormalizado en `experiencias` por
     * rendimiento de lectura (ver Experiencia.java).
     */
    @Query("""
            select coalesce(sum(r.cantidadPersonas), 0)
            from Reserva r
            where r.experiencia.id = :experienciaId
              and r.estado.nombre = 'CONFIRMADA'
            """)
    long sumPersonasConfirmadasPorExperiencia(@Param("experienciaId") Long experienciaId);
}
