# ============================================
# STAGE 1 — BUILD
# ============================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copier d'abord le pom.xml seul (optimisation cache Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copier le code source et compiler
COPY src ./src
RUN mvn clean package -DskipTests -q

# ============================================
# STAGE 2 — RUNTIME
# ============================================
FROM eclipse-temurin:21-jre-alpine

# Créer un utilisateur non-root (sécurité)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copier UNIQUEMENT le .jar depuis le stage builder
COPY --from=builder /app/target/*.jar app.jar

# Donner les droits à appuser
RUN chown appuser:appgroup app.jar

# Basculer vers l'utilisateur non-root
USER appuser

# Port exposé
EXPOSE 8080

# Vérification santé de l'app
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/api/v1/actuator/health || exit 1

# Lancer l'app
ENTRYPOINT ["java", "-jar", "app.jar"]