package com.sgi.auto.reportes;

import com.sgi.auto.compartido.ApiRespuesta;
import com.sgi.auto.reportes.dto.DashboardDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RF-096 al RF-102
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DUENO')")
public class DashboardControlador {

    private final DashboardServicio dashboardServicio;

    @GetMapping
    public ResponseEntity<ApiRespuesta<DashboardDTO>> dashboard() {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(dashboardServicio.obtenerDashboard()));
    }
}
