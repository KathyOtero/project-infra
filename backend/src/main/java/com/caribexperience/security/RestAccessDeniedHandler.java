package com.caribexperience.security;

import com.caribexperience.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Se activa cuando el usuario SI esta autenticado (tiene un JWT valido) pero
 * su rol no cumple el @PreAuthorize del recurso solicitado (403 Forbidden).
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "Forbidden",
                "No tienes permisos para realizar esta accion", request.getRequestURI());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
