package com.sgi.auto.compartido;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint público de salud, usado por el Health Check de Render
 * para confirmar que el servicio está vivo.
 */
@RestController
public class SaludControlador {

    @GetMapping("/api/salud")
    public ResponseEntity<ApiRespuesta<String>> salud() {
        return ResponseEntity.ok(ApiRespuesta.exitoso("Almacén y Servicios Eléctricos DB operativo"));
    }
}