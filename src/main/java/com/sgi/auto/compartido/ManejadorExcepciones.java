package com.sgi.auto.compartido;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ManejadorExcepciones {

    // Errores de validación de campos (@Valid en DTOs)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiRespuesta<Void>> manejarValidacion(
            MethodArgumentNotValidException ex) {

        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .badRequest()
                .body(ApiRespuesta.error("Error de validación: " + mensaje));
    }

    // Credenciales incorrectas en el login
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiRespuesta<Void>> manejarCredenciales(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiRespuesta.error("Credenciales incorrectas"));
    }

    // Acceso denegado por permisos
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiRespuesta<Void>> manejarAccesoDenegado(AuthorizationDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiRespuesta.error("No tiene permisos para realizar esta operación"));
    }

    // Recurso no encontrado
    @ExceptionHandler(RecursoNoEncontradoExcepcion.class)
    public ResponseEntity<ApiRespuesta<Void>> manejarNoEncontrado(RecursoNoEncontradoExcepcion ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiRespuesta.error(ex.getMessage()));
    }

    // Regla de negocio violada (stock insuficiente, etc.)
    @ExceptionHandler(ReglaNegocioExcepcion.class)
    public ResponseEntity<ApiRespuesta<Void>> manejarReglaNegocio(ReglaNegocioExcepcion ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiRespuesta.error(ex.getMessage()));
    }

    // Conflicto (código duplicado, clave idempotencia repetida, etc.)
    @ExceptionHandler(ConflictoExcepcion.class)
    public ResponseEntity<ApiRespuesta<Void>> manejarConflicto(ConflictoExcepcion ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiRespuesta.error(ex.getMessage()));
    }

    // Cualquier error inesperado — log del stack trace, mensaje genérico al cliente
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiRespuesta<Void>> manejarGeneral(Exception ex) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiRespuesta.error("Error interno del servidor. Contacte al administrador."));
    }
}