package com.sgi.auto.reportes;

import com.sgi.auto.compartido.ApiRespuesta;
import com.sgi.auto.reportes.dto.DashboardDTO;
import com.sgi.auto.reportes.dto.DashboardMecanicoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardControlador {

    private final DashboardServicio dashboardServicio;

    @GetMapping
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<ApiRespuesta<DashboardDTO>> dashboard() {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(dashboardServicio.obtenerDashboard()));
    }

    @GetMapping("/mecanico")
    @PreAuthorize("hasRole('MECANICO')")
    public ResponseEntity<ApiRespuesta<DashboardMecanicoDTO>> dashboardMecanico(
            Authentication authentication) {
        return ResponseEntity.ok(
                ApiRespuesta.exitoso(dashboardServicio.obtenerDashboardMecanico(
                        authentication.getName())));
    }
}