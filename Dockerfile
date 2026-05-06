# ================================
# Stage 1: Build
# ================================
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first (layer caching — only re-downloads
# dependencies when pom.xml actually changes)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copy source code and build the jar (skip tests — tests run in CI, not here)
COPY src/ src/

RUN ./mvnw clean package -DskipTests -q

# ================================
# Stage 2: Run
# ================================
FROM eclipse-temurin:25-jre

WORKDIR /app

# Create a non-root user for security (never run as root in production)
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

# Copy only the built jar from the builder stage (keeps final image small)
COPY --from=builder /app/target/HeliosGreenLightSolar-0.0.1.jar app.jar

# Give ownership to non-root user
RUN chown appuser:appgroup app.jar

USER appuser

# Render injects the PORT env variable; default to 8080 locally
EXPOSE 8080

# JVM flags:
#   -XX:+UseContainerSupport        → respect Docker CPU/memory limits
#   -XX:MaxRAMPercentage=75.0       → use up to 75% of container RAM for heap
#   -Djava.security.egd=...         → faster startup (avoids /dev/random blocking)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]