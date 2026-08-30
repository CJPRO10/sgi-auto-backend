package com.sgi.auto.backup;

import com.sgi.auto.backup.dto.RespaldoRespuestaDTO;
import com.sgi.auto.compartido.RecursoNoEncontradoExcepcion;
import com.sgi.auto.notificaciones.NotificacionServicio;
import com.sgi.auto.notificaciones.TipoNotificacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Servicio de backups. RF-091 al RF-095.
 *
 * El dump se genera con pg_dump en un archivo temporal, se sube de
 * inmediato a Backblaze B2 (almacenamiento externo persistente) y el
 * archivo local se borra. Nunca se depende del filesystem del
 * contenedor más allá del momento de la subida, porque en Render (y en
 * la mayoría de hostings tipo contenedor) el disco es efímero y se
 * pierde en cada reinicio o redeploy.
 *
 * Se dispara en 3 momentos:
 *  - Automáticamente todos los días a las 2:00 a.m.
 *  - Automáticamente cada vez que se cierra una sesión de caja (async,
 *    nunca bloquea ni puede hacer fallar el cierre de caja).
 *  - Manualmente cuando el DUEÑO lo solicita desde la app.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupServicio {

    private final RespaldoRepositorio respaldoRepositorio;
    private final NotificacionServicio notificacionServicio;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/sgi_auto}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:postgres}")
    private String dbUsuario;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${sgi.s3.bucket}")
    private String bucket;

    private static final String CARPETA_S3 = "backups/";
    private static final String DIRECTORIO_TEMPORAL = "/tmp/";
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final int RETENCION_MAXIMA = 30; // RF-092: últimos 30 backups
    private static final int MINUTOS_VALIDEZ_DESCARGA = 15; // RF-093

    // ── Disparadores ──────────────────────────────────────────

    /** Backup automático diario a las 2:00 a.m. RF-091. */
    @Scheduled(cron = "0 0 2 * * *")
    public void backupAutomatico() {
        log.info("Iniciando backup automático programado...");
        ejecutarBackup("auto");
    }

    /** Backup manual disparado por el DUEÑO desde la app. RF-093. */
    public RespaldoRespuestaDTO backupManual() {
        log.info("Backup manual iniciado por el dueño");
        return ejecutarBackup("manual");
    }

    /**
     * Backup disparado automáticamente al cerrar una sesión de caja.
     * Se ejecuta como @Async y solo DESPUÉS de que la transacción de
     * cierre haya confirmado por completo (AFTER_COMMIT) — así el
     * backup incluye con certeza ese cierre, y nunca se dispara si la
     * transacción terminó en rollback. Cualquier error queda registrado
     * en el historial de respaldos y notificado, pero nunca se propaga
     * ni afecta el cierre de caja, que ya ocurrió y quedó confirmado.
     */
    @Async
    @org.springframework.transaction.event.TransactionalEventListener(
            phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void alCerrarCaja(com.sgi.auto.caja.CajaCerradaEvento evento) {
        try {
            log.info("Backup automático disparado por cierre de caja (sesión {})", evento.sesionId());
            ejecutarBackup("cierre-caja");
        } catch (Exception e) {
            log.error("Fallo inesperado en backup por cierre de caja: {}", e.getMessage(), e);
        }
    }

    // ── Lógica principal ──────────────────────────────────────

    private RespaldoRespuestaDTO ejecutarBackup(String origen) {
        String timestamp = LocalDateTime.now().format(FORMATO_FECHA);
        String nombreArchivo = origen + "-" + timestamp + ".sql";
        File archivoTemporal = new File(DIRECTORIO_TEMPORAL + nombreArchivo);
        String claveS3 = CARPETA_S3 + nombreArchivo;

        try {
            generarDump(archivoTemporal);

            long tamano = archivoTemporal.length();
            if (tamano == 0) {
                throw new IOException("pg_dump generó un archivo vacío");
            }

            subirAS3(archivoTemporal, claveS3);
            limpiarBackupsAntiguos();

            Respaldo respaldo = Respaldo.builder()
                    .nombreArchivo(nombreArchivo)
                    .rutaAlmacenamiento(claveS3)
                    .tamanoBytes(tamano)
                    .exitoso(true)
                    .build();
            Respaldo guardado = respaldoRepositorio.save(respaldo);

            notificacionServicio.enviar(
                    TipoNotificacion.BACKUP_EXITOSO,
                    "Backup completado",
                    "Backup " + nombreArchivo + " generado y almacenado correctamente (" +
                            (tamano / 1024) + " KB)",
                    null, "Respaldo", guardado.getId());

            log.info("Backup exitoso: {}, tamaño: {} bytes, s3Key: {}",
                    nombreArchivo, tamano, claveS3);
            return aDTO(guardado);

        } catch (Exception e) {
            log.error("Error en backup: {}", e.getMessage(), e);

            Respaldo respaldoFallido = Respaldo.builder()
                    .nombreArchivo(nombreArchivo)
                    .rutaAlmacenamiento(claveS3)
                    .exitoso(false)
                    .mensajeError(e.getMessage())
                    .build();
            Respaldo guardado = respaldoRepositorio.save(respaldoFallido);

            notificacionServicio.enviar(
                    TipoNotificacion.BACKUP_FALLIDO,
                    "Error en backup",
                    "El backup falló: " + e.getMessage(),
                    null, "Respaldo", guardado.getId());

            return aDTO(guardado);
        } finally {
            // El disco del contenedor es efímero de todas formas, pero
            // limpiamos explícitamente para no acumular basura mientras
            // la instancia esté viva.
            if (archivoTemporal.exists() && !archivoTemporal.delete()) {
                log.warn("No se pudo eliminar el archivo temporal: {}", archivoTemporal);
            }
        }
    }

    private void generarDump(File destino) throws IOException, InterruptedException {
        String urlSinJdbc = datasourceUrl.replace("jdbc:postgresql://", "");
        String[] partes = urlSinJdbc.split("/");
        String hostPuerto = partes[0];
        String baseDatos = partes.length > 1 ? partes[1].split("\\?")[0] : "sgi_auto";
        String host = hostPuerto.contains(":") ? hostPuerto.split(":")[0] : hostPuerto;
        String puerto = hostPuerto.contains(":") ? hostPuerto.split(":")[1] : "5432";

        ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "-h", host,
                "-p", puerto,
                "-U", dbUsuario,
                "-d", baseDatos,
                "-f", destino.getAbsolutePath(),
                "--no-password"
        );
        pb.environment().put("PGPASSWORD", dbPassword);
        pb.redirectErrorStream(true);

        Process proceso = pb.start();
        String salida = new String(proceso.getInputStream().readAllBytes());
        int exitCode = proceso.waitFor();

        if (exitCode != 0) {
            throw new IOException("pg_dump terminó con código " + exitCode + ": " + salida);
        }
    }

    private void subirAS3(File archivo, String clave) throws IOException {
        PutObjectRequest peticion = PutObjectRequest.builder()
                .bucket(bucket)
                .key(clave)
                .contentType("application/sql")
                .build();
        s3Client.putObject(peticion, RequestBody.fromFile(archivo));
    }

    // Mantener solo los últimos 30 backups en el almacenamiento externo.
    private void limpiarBackupsAntiguos() {
        try {
            ListObjectsV2Request listado = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(CARPETA_S3)
                    .build();

            List<S3Object> objetos = s3Client.listObjectsV2(listado).contents().stream()
                    .sorted(Comparator.comparing(S3Object::lastModified).reversed())
                    .toList();

            if (objetos.size() <= RETENCION_MAXIMA) return;

            List<S3Object> aEliminar = objetos.subList(RETENCION_MAXIMA, objetos.size());
            for (S3Object obj : aEliminar) {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(obj.key())
                        .build());
                log.info("Backup antiguo eliminado por retención: {}", obj.key());
            }
        } catch (Exception e) {
            // Un fallo en la limpieza NUNCA debe hacer fallar el backup que
            // ya se generó y subió correctamente. Solo se registra.
            log.warn("No se pudo aplicar la retención de backups antiguos: {}", e.getMessage());
        }
    }

    // ── Consultas / descarga ──────────────────────────────────

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<RespaldoRespuestaDTO> listarRespaldos(Pageable pageable) {
        return respaldoRepositorio.listarTodos(pageable).map(this::aDTO);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public RespaldoRespuestaDTO obtenerPorId(Long id) {
        return aDTO(buscarOLanzar(id));
    }

    /**
     * Genera una URL temporal firmada para descargar el backup directamente
     * desde Backblaze, sin pasar el archivo por el backend.
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String generarUrlDescarga(Long id) {
        Respaldo respaldo = buscarOLanzar(id);

        if (!respaldo.isExitoso()) {
            throw new com.sgi.auto.compartido.ReglaNegocioExcepcion(
                    "No se puede descargar un backup que falló");
        }

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(respaldo.getRutaAlmacenamiento())
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(MINUTOS_VALIDEZ_DESCARGA))
                .getObjectRequest(getRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private Respaldo buscarOLanzar(Long id) {
        return respaldoRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "No se encontró el respaldo con id: " + id));
    }

    private RespaldoRespuestaDTO aDTO(Respaldo r) {
        return new RespaldoRespuestaDTO(
                r.getId(), r.getNombreArchivo(), r.getRutaAlmacenamiento(),
                r.getTamanoBytes(), r.isExitoso(), r.getMensajeError(), r.getCreadoEn());
    }
}