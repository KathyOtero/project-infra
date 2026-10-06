package com.caribexperience.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones (patron Controller Advice). Centraliza la
 * traduccion de excepciones de negocio a codigos HTTP, para que ningun
 * controlador tenga que hacer try/catch manual.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(OperacionNoAutorizadaException.class)
    public ResponseEntity<ErrorResponse> manejarNoAutorizado(OperacionNoAutorizadaException ex, HttpServletRequest req) {
        return construirRespuesta(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    /** Credenciales invalidas en /api/auth/login (email no existe o password incorrecto). */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> manejarAutenticacion(AuthenticationException ex, HttpServletRequest req) {
        return construirRespuesta(HttpStatus.UNAUTHORIZED, "Email o contrasena incorrectos", req);
    }

    /** Usuario autenticado pero sin el rol requerido (@PreAuthorize) para la operacion. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> manejarAccesoDenegado(AccessDeniedException ex, HttpServletRequest req) {
        return construirRespuesta(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta accion", req);
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<ErrorResponse> manejarReglaDeNegocio(ReglaDeNegocioException ex, HttpServletRequest req) {
        // Cubre tambien las subclases: EmailYaRegistradoException, CupoInsuficienteException
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ErrorResponse body = ErrorResponse.deValidacion(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                "Error de validacion en los datos enviados", req.getRequestURI(), errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> manejarViolacionRestriccion(ConstraintViolationException ex, HttpServletRequest req) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarJsonMalformado(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es un JSON valido", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorGenerico(Exception ex, HttpServletRequest req) {
        // Nunca se expone ex.getMessage() de una excepcion no controlada al cliente
        // (podria filtrar detalles internos); se registra en logs para diagnostico.
        log.error("Error no controlado en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error inesperado. Contacte al administrador.", req);
    }

    private ResponseEntity<ErrorResponse> construirRespuesta(HttpStatus status, String mensaje, HttpServletRequest req) {
        ErrorResponse body = ErrorResponse.of(status.value(), status.getReasonPhrase(), mensaje, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
