package com.sgi.auto.backup;

import com.sgi.auto.backup.dto.RespaldoRespuestaDTO;
import com.sgi.auto.notificaciones.NotificacionServicio;
import com.sgi.auto.notificaciones.TipoNotificacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Servicio de backups automáticos.
 * RF-091 al RF-095
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupServicio {

    private final RespaldoRepositorio respaldoRepositorio;
    private final NotificacionServicio notificacionServicio;

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/sgi_auto}")
    private String datasourceUrl;

    @Value("${SPRING_DATASOURCE_USERNAME:postgres}")
    private String dbUsuario;

    @Value("${SPRING_DATASOURCE_PASSWORD:}")
    private String dbPassword;

    private static final String DIRECTORIO_BACKUP = "/tmp/backups/sgi-auto/";
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * Backup automático diario a las 2:00 AM.
     * RF-091
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void backupAutomatico() {
        log.info("Iniciando backup automático programado...");
        ejecutarBackup();
    }

    /**
     * Backup manual disparado por el DUEÑO.
     * RF-093
     */
    public RespaldoRespuestaDTO backupManual() {
        log.info("Backup manual iniciado por usuario");
        return ejecutarBackup();
    }

    private RespaldoRespuestaDTO ejecutarBackup() {
        String timestamp = LocalDateTime.now().format(FORMATO_FECHA);
        String nombreArchivo = "sgi-auto-backup-" + timestamp + ".sql";
        String rutaCompleta = DIRECTORIO_BACKUP + nombreArchivo;

        try {
            // Crear directorio si no existe
            new File(DIRECTORIO_BACKUP).mkdirs();

            // Extraer datos de conexión de la URL JDBC
            String urlSinJdbc = datasourceUrl.replace("jdbc:postgresql://", "");
            String[] partes = urlSinJdbc.split("/");
            String hostPuerto = partes[0];
            String baseDatos = partes.length > 1 ? partes[1] : "sgi_auto";

            // Ejecutar pg_dump
            ProcessBuilder pb = new ProcessBuilder(
                    "pg_dump",
                    "-h", hostPuerto.contains(":") ? hostPuerto.split(":")[0] : hostPuerto,
                    "-p", hostPuerto.contains(":") ? hostPuerto.split(":")[1] : "5432",
                    "-U", dbUsuario,
                    "-d", baseDatos,
                    "-f", rutaCompleta,
                    "--no-password"
            );
            pb.environment().put("PGPASSWORD", dbPassword);
            pb.redirectErrorStream(true);

            Process proceso = pb.start();
            int exitCode = proceso.waitFor();

            if (exitCode != 0) {
                throw new IOException("pg_dump terminó con código: " + exitCode);
            }

            File archivoBackup = new File(rutaCompleta);
            long tamano = archivoBackup.length();

            Respaldo respaldo = Respaldo.builder()
                    .nombreArchivo(nombreArchivo)
                    .rutaAlmacenamiento(rutaCompleta)
                    .tamanoBytes(tamano)
                    .exitoso(true)
                    .build();

            Respaldo guardado = respaldoRepositorio.save(respaldo);

            // RF-094 — Notificar éxito
            notificacionServicio.enviar(
                    TipoNotificacion.BACKUP_EXITOSO,
                    "Backup completado",
                    "Backup " + nombreArchivo + " generado correctamente (" +
                            (tamano / 1024) + " KB)",
                    null, "Respaldo", guardado.getId());

            log.info("Backup exitoso: {}, tamaño: {} bytes", nombreArchivo, tamano);
            return aDTO(guardado);

        } catch (Exception e) {
            log.error("Error en backup: {}", e.getMessage(), e);

            Respaldo respaldoFallido = Respaldo.builder()
                    .nombreArchivo(nombreArchivo)
                    .rutaAlmacenamiento(rutaCompleta)
                    .exitoso(false)
                    .mensajeError(e.getMessage())
                    .build();

            Respaldo guardado = respaldoRepositorio.save(respaldoFallido);

            // RF-094 — Notificar fallo
            notificacionServicio.enviar(
                    TipoNotificacion.BACKUP_FALLIDO,
                    "Error en backup",
                    "El backup falló: " + e.getMessage(),
                    null, "Respaldo", guardado.getId());

            return aDTO(guardado);
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<RespaldoRespuestaDTO> listarRespaldos(Pageable pageable) {
        return respaldoRepositorio.listarTodos(pageable).map(this::aDTO);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public RespaldoRespuestaDTO obtenerPorId(Long id) {
        return respaldoRepositorio.findById(id)
                .map(this::aDTO)
                .orElseThrow(() -> new com.sgi.auto.compartido.RecursoNoEncontradoExcepcion(
                        "No se encontró el respaldo con id: " + id));
    }

    private RespaldoRespuestaDTO aDTO(Respaldo r) {
        return new RespaldoRespuestaDTO(
                r.getId(), r.getNombreArchivo(), r.getRutaAlmacenamiento(),
                r.getTamanoBytes(), r.isExitoso(), r.getMensajeError(), r.getCreadoEn());
    }
}