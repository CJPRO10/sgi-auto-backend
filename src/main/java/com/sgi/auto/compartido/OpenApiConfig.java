package com.sgi.auto.compartido;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la documentación OpenAPI (Swagger).
 * Disponible en /api/docs (JSON) y /api/swagger-ui (interfaz interactiva).
 *
 * Define el esquema de seguridad Bearer JWT para que Swagger UI
 * permita probar endpoints protegidos directamente desde el navegador.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI configuracionOpenApi() {
        return new OpenAPI()
                .info(infoApi())
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_JWT, esquemaSeguridadJwt()));
    }

    private Info infoApi() {
        return new Info()
                .title("SGI-AUTO — API REST")
                .description("""
                        Sistema de Gestión Integral para almacén de repuestos \
                        eléctricos y taller automotriz.

                        Para probar endpoints protegidos:
                        1. Use POST /api/autenticacion/ingresar para obtener un token JWT.
                        2. Haga clic en el botón 'Authorize' (candado) arriba a la derecha.
                        3. Pegue el token (sin la palabra 'Bearer', solo el token).
                        4. Todos los endpoints protegidos usarán ese token automáticamente.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Camilo Jiménez")
                        .url("https://github.com/CJPRO10"));
    }

    private SecurityScheme esquemaSeguridadJwt() {
        return new SecurityScheme()
                .name(ESQUEMA_JWT)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Pegue aquí el token JWT obtenido del endpoint de login");
    }
}