package com.caribexperience.security;

import com.caribexperience.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Se activa cuando un request llega SIN autenticacion a un endpoint
 * protegido (401 Unauthorized) -- distinto de AccessDeniedHandler, que se
 * activa cuando el usuario SI esta autenticado pero no tiene el rol
 * requerido (403 Forbidden). Sin este bean, Spring Security devuelve una
 * pagina HTML de login por defecto en vez de JSON.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
                "Se requiere autenticacion para acceder a este recurso", request.getRequestURI());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
