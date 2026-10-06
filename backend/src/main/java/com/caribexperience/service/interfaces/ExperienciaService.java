package com.caribexperience.service.interfaces;

import com.caribexperience.web.dto.experiencia.ExperienciaFiltro;
import com.caribexperience.web.dto.experiencia.ExperienciaRequest;
import com.caribexperience.web.dto.experiencia.ExperienciaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExperienciaService {

    /** Crea una experiencia nueva. Solo puede invocarlo un usuario autenticado con rol GUIA. */
    ExperienciaResponse crear(Long guiaId, ExperienciaRequest request);

    /** Actualiza una experiencia existente. Valida que el guia autenticado sea el dueno. */
    ExperienciaResponse actualizar(Long experienciaId, Long guiaId, ExperienciaRequest request);

    /** Cancela (borrado logico) una experiencia. Valida que el guia autenticado sea el dueno. */
    void cancelar(Long experienciaId, Long guiaId);

    ExperienciaResponse obtenerPorId(Long experienciaId);

    /** Listado publico con filtros opcionales (ciudad, categoria, rango de fechas). */
    Page<ExperienciaResponse> buscar(ExperienciaFiltro filtro, Pageable pageable);

    /** Experiencias publicadas por un guia especifico ("mis experiencias"). */
    Page<ExperienciaResponse> listarPorGuia(Long guiaId, Pageable pageable);
}
