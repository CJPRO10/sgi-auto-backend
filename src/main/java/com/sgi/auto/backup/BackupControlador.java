package com.sgi.auto.backup;

import com.sgi.auto.backup.dto.RespaldoRespuestaDTO;
import com.sgi.auto.compartido.ApiRespuesta;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * RF-091 al RF-095
 */
@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DUENO')")
public class BackupControlador {

    private final BackupServicio backupServicio;

    @PostMapping("/ejecutar")
    public ResponseEntity<ApiRespuesta<RespaldoRespuestaDTO>> ejecutarBackup() {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                backupServicio.backupManual(),
                "Backup iniciado correctamente"));
    }

    @GetMapping
    public ResponseEntity<ApiRespuesta<Page<RespaldoRespuestaDTO>>> listar(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                backupServicio.listarRespaldos(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiRespuesta<RespaldoRespuestaDTO>> obtenerPorId(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiRespuesta.exitoso(
                backupServicio.obtenerPorId(id)));
    }
}