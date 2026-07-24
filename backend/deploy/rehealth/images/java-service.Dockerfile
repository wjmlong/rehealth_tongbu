FROM eclipse-temurin:17.0.16_8-jre-jammy@sha256:e90fd2b084488c0ffdd22610d44c1579e63b585c7fb83489fd815c0dc5b4e1f7

ARG JAR_FILE
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates wget \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system rehealth \
    && useradd --system --gid rehealth --home-dir /opt/rehealth rehealth \
    && mkdir -p /opt/rehealth/logs/nacos /opt/logs \
    && chown -R rehealth:rehealth /opt/rehealth /opt/logs
WORKDIR /opt/rehealth
COPY ${JAR_FILE} app.jar
USER rehealth
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.security.egd=file:/dev/urandom", "-jar", "/opt/rehealth/app.jar"]
