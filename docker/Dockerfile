
FROM openjdk:11-jre AS builder

ARG BACKEND_APP
ARG BACKEND_ZIP="$BACKEND_APP.zip"

COPY $BACKEND_ZIP /opt/
WORKDIR /opt
RUN unzip $BACKEND_ZIP && rm $BACKEND_ZIP


FROM openjdk:21

COPY --from=builder /opt/$BACKEND_APP /opt/$BACKEND_APP

COPY --chmod=755 entrypoint.sh /


LABEL org.opencontainers.image.licenses=MIT
LABEL org.opencontainers.image.source=https://github.com/dnpm-dip/api-gateway
LABEL org.opencontainers.image.description="DNPM:DIP Backend API Gateway"

ARG CONFIG_DIR=/dnpm_config
ARG DATA_DIR=/dnpm_data
ARG BACKEND_APP=$BACKEND_APP

ENV CONFIG_DIR=/dnpm_config
ENV DATA_DIR=/dnpm_data
ENV BACKEND_APP=$BACKEND_APP
ENV PID_FILE=/dev/null
ENV HTTP_PORT=9000
ENV HATEOAS_HOST=""
ENV AUTHUP_URL=""
ENV APPLICATION_SECRET="$(head -c 64 /dev/urandom | base64)"
ENV RD_RANDOM_DATA=-1
ENV MTB_RANDOM_DATA=-1
ENV CONNECTOR_TYPE="broker"
ENV JAVA_OPTS="-Xmx2g"

VOLUME $CONFIG_DIR
VOLUME $DATA_DIR

EXPOSE 9000

HEALTHCHECK --interval=10s --timeout=5s --retries=5 --start-period=5s \
  CMD curl http://127.0.0.1:9000/peer2peer/status || exit 1


ENTRYPOINT ["/entrypoint.sh"]

