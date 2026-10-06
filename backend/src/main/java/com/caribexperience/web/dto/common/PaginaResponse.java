package com.caribexperience.web.dto.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envoltorio explicito para respuestas paginadas. Se prefiere sobre exponer
 * org.springframework.data.domain.Page directamente en la API: Page expone
 * detalles internos de Spring Data (Pageable, Sort) que no son parte del
 * contrato publico y generan advertencias de Spring Boot en produccion.
 */
public record PaginaResponse<T>(
        List<T> contenido,
        int paginaActual,
        int totalPaginas,
        long totalElementos,
        boolean esUltimaPagina
) {
    public static <T> PaginaResponse<T> de(Page<T> pagina) {
        return new PaginaResponse<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getTotalPages(),
                pagina.getTotalElements(),
                pagina.isLast()
        );
    }
}
