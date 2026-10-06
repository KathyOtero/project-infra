package com.caribexperience.security;

import com.caribexperience.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Filtro que se ejecuta una vez por request (antes del filtro estandar de
 * usuario/password de Spring Security). Busca el header "Authorization:
 * Bearer <token>", lo valida y, si es correcto, deja al usuario autenticado
 * en el SecurityContext para el resto de la cadena de filtros/controladores.
 *
 * Si el token es invalido/expirado, NO lanza la excepcion hacia arriba (el
 * GlobalExceptionHandler @RestControllerAdvice no aplica aqui, porque los
 * filtros corren antes de que Spring MVC entre en accion); en su lugar se
 * escribe directamente una respuesta 401 con el mismo formato ErrorResponse
 * que usa el resto de la API, manteniendo la consistencia para el frontend.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            String email = jwtService.extraerEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsuarioPrincipal principal = (UsuarioPrincipal) userDetailsService.loadUserByUsername(email);

                if (jwtService.esTokenValido(token, principal)) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token JWT invalido o expirado: {}", ex.getMessage());
            escribirRespuestaNoAutorizada(response, request.getRequestURI());
        }
    }

    private void escribirRespuestaNoAutorizada(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
                "Token invalido o expirado", path);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
