# ── Etapa 1: Compilar con Maven ──────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar el pom.xml primero para aprovechar el caché de capas de Docker
# (si no cambió el pom.xml, no vuelve a descargar dependencias)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copiar el código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Etapa 2: Imagen de producción ligera ─────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# pg_dump es necesario para generar los backups de la base de datos.
# Se intenta la versión específica (debe coincidir con la versión del
# servidor Postgres, actualmente 16.x) y si el repositorio de Alpine
# de esa imagen no la tiene disponible, cae al paquete genérico.
RUN apk add --no-cache postgresql16-client || apk add --no-cache postgresql-client

# Copiar solo el JAR generado (sin el código fuente ni Maven)
COPY --from=build /app/target/auto-1.0.0.jar app.jar

# Puerto que expone la aplicación
EXPOSE 8080

# Comando de arranque
ENTRYPOINT ["java", "-jar", "app.jar"]