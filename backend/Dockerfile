# syntax=docker/dockerfile:1

# ---------- Etapa de build ----------
# Se compila dentro de un contenedor con Maven + JDK 17 para no depender de
# tener Maven instalado en la maquina que construye la imagen (CI/CD friendly).
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos primero solo el pom.xml para aprovechar la cache de capas de Docker:
# si las dependencias no cambian, "mvn dependency:go-offline" no se re-ejecuta
# aunque cambie el codigo fuente.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Etapa de runtime ----------
# Imagen final minima: solo el JRE (no el JDK completo) y el .jar ya compilado.
# Se usa la variante "jammy" (Ubuntu) en vez de "alpine" porque tiene soporte
# multi-arquitectura mas confiable (amd64/arm64), relevante ya que el equipo
# desarrolla en Mac Apple Silicon y desplegara en EC2 (amd64).
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Usuario no-root por buenas practicas de seguridad en produccion.
RUN groupadd -r caribe && useradd -r -g caribe caribe
USER caribe

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Health check compatible con el actuator ya expuesto en application.yml,
# util para que el Load Balancer / orquestador sepa cuando el contenedor
# esta realmente listo para recibir trafico.
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
