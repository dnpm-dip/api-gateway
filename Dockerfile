FROM openjdk:11-jre AS builder

# SBT installieren
RUN apt-get update && apt-get install -y curl && \
    curl -L -o sbt.deb https://scala.jfrog.io/artifactory/debian/sbt-1.11.3.deb && \
    apt-get install -y ./sbt.deb && \
    rm sbt.deb

WORKDIR /app

# SBT-Cache vorbereiten (erst nur project files kopieren)
COPY build.sbt .
COPY project/ ./project/

ARG GITHUB_TOKEN=""
# Github Token
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

# Dependencies cachen
RUN sbt update

# Source-Code kopieren und kompilieren
COPY . .
RUN sbt package


FROM openjdk:21

COPY --from=builder /app/target/scala-*/dnpm-dip-api-gateway-*.jar app.jar

COPY --chmod=755 entrypoint.sh /


LABEL org.opencontainers.image.licenses=MIT
LABEL org.opencontainers.image.source=https://github.com/dnpm-dip/api-gateway
LABEL org.opencontainers.image.description="DNPM:DIP Backend API Gateway"

ARG CONFIG_DIR=/dnpm_config
ARG DATA_DIR=/dnpm_data
ARG GITHUB_TOKEN=""

ENV CONFIG_DIR=/dnpm_config
ENV DATA_DIR=/dnpm_data
ENV PID_FILE=/dev/null
ENV HTTP_PORT=9000
ENV HATEOAS_HOST=""
ENV AUTHUP_URL=""
ENV APPLICATION_SECRET="$(head -c 64 /dev/urandom | base64)"
ENV RD_RANDOM_DATA=-1
ENV MTB_RANDOM_DATA=-1
ENV CONNECTOR_TYPE="broker"
ENV JAVA_OPTS="-Xmx2g"
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

VOLUME $CONFIG_DIR
VOLUME $DATA_DIR

EXPOSE 9000

HEALTHCHECK --interval=10s --timeout=5s --retries=5 --start-period=5s \
  CMD curl http://127.0.0.1:9000/peer2peer/status || exit 1


ENTRYPOINT ["/entrypoint.sh"]

