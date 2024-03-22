
FROM openjdk:11-jre AS builder

ARG BACKEND_APP
ARG BACKEND_ZIP="$BACKEND_APP.zip"

COPY $BACKEND_ZIP /opt/
WORKDIR /opt
RUN unzip $BACKEND_ZIP && rm $BACKEND_ZIP


FROM openjdk:21

COPY --from=builder /opt/$BACKEND_APP /opt/$BACKEND_APP

LABEL org.opencontainers.image.licenses=MIT
LABEL org.opencontainers.image.source=https://github.com/KohlbacherLab/dnpm-dip-api-gateway
LABEL org.opencontainers.image.description="DNPM:DIP Backend API Gateway"

ARG CONFIG_DIR=/dnpm_config
ARG DATA_DIR=/dnpm_data

ENV CONFIG_DIR=/dnpm_config
ENV DATA_DIR=/dnpm_data

ARG BACKEND_APP=$BACKEND_APP
ENV BACKEND_APP=$BACKEND_APP

ENV PID_FILE=/dev/null
ENV HTTP_PORT=9000
ENV HATEOAS_HOST=""
ENV AUTHUP_URL=""
ENV APPLICATION_SECRET="$(head -c 64 /dev/urandom | base64)"
ENV RD_RANDOM_DATA=-1
ENV MTB_RANDOM_DATA=-1
ENV CONNECTOR_TYPE="broker"


USER 999

VOLUME $CONFIG_DIR
VOLUME $DATA_DIR

EXPOSE 9000

ENTRYPOINT /opt/$BACKEND_APP/bin/dnpm-dip-api-gateway \
    -Dhttp.port="$HTTP_PORT" \
    -Dplay.http.secret.key="$APPLICATION_SECRET" \
    -Dconfig.file="$CONFIG_DIR/production.conf" \
    -Dpidfile.path="$PID_FILE" \
    -Dlogger.file="$CONFIG_DIR/logback.xml" \
    -Ddnpm.dip.config.file="$CONFIG_DIR/config.xml" \
    -Ddnpm.dip.authup.url="$AUTHUP_URL" \
    -Ddnpm.dip.rest.api.baseurl="$HATEOAS_HOST" \
    -Ddnpm.dip.connector.type="$CONNECTOR_TYPE" \
    -Ddnpm.dip.query.data.dir="$DATA_DIR" \
    -Ddnpm.dip.rd.query.data.generate="$RD_RANDOM_DATA" \
    -Ddnpm.dip.mtb.query.data.generate="$MTB_RANDOM_DATA"
