# TP3 - Image de l'application (le JAR est produit au préalable par "mvn package")
FROM eclipse-temurin:21-jre-jammy

ARG APP_VERSION=1.0.0
ENV APP_VERSION=${APP_VERSION} \
    JAVA_OPTS="-Xms128m -Xmx384m"

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --create-home appuser

WORKDIR /app
COPY target/devops-demo.jar app.jar

USER appuser
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=45s --retries=3 \
  CMD curl -fs http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
