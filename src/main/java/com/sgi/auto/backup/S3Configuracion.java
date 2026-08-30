package com.sgi.auto.backup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Configura el cliente S3 para usar Backblaze B2 (API compatible con S3)
 * en vez de AWS real. Backblaze exige acceso "path-style" y una región
 * válida para la firma de las peticiones (SigV4), que extraemos del
 * propio endpoint (formato: s3.<region>.backblazeb2.com).
 */
@Configuration
public class S3Configuracion {

    @Value("${sgi.s3.endpoint}")
    private String endpoint;

    @Value("${sgi.s3.access-key}")
    private String accessKey;

    @Value("${sgi.s3.secret-key}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(endpointUri())
                .region(regionDesdeEndpoint())
                .credentialsProvider(StaticCredentialsProvider.create(credenciales()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(endpointUri())
                .region(regionDesdeEndpoint())
                .credentialsProvider(StaticCredentialsProvider.create(credenciales()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private AwsBasicCredentials credenciales() {
        return AwsBasicCredentials.create(accessKey, secretKey);
    }

    private URI endpointUri() {
        String url = endpoint.startsWith("http") ? endpoint : "https://" + endpoint;
        return URI.create(url);
    }

    // Extrae la región del endpoint (ej. "s3.us-east-005.backblazeb2.com" -> "us-east-005").
    // Si no logra parsearlo, usa un valor genérico como respaldo.
    private Region regionDesdeEndpoint() {
        try {
            String host = endpoint.replace("https://", "").replace("http://", "");
            String[] partes = host.split("\\.");
            if (partes.length >= 4 && partes[0].equals("s3")) {
                return Region.of(partes[1]);
            }
        } catch (Exception ignorado) {
            // usa el valor por defecto de abajo
        }
        return Region.US_EAST_1;
    }
}