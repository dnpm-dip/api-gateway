ARG JAVA_VERSION=11

FROM openjdk:${JAVA_VERSION}-jre

ARG BACKEND_APP
ARG BACKEND_ZIP="$BACKEND_APP.zip"


LABEL org.opencontainers.image.licenses=MIT
LABEL org.opencontainers.image.source=https://github.com/KohlbacherLab/dnpm-dip-api-gateway
LABEL org.opencontainers.image.description="DNPM:DIP Backend API Gateway"


COPY $BACKEND_ZIP /opt/
WORKDIR /opt
RUN unzip $BACKEND_ZIP && rm $BACKEND_ZIP


ARG CONFIG_DIR=/config

ENV BACKEND_APP=$BACKEND_APP

ENV PID_FILE=/dev/null
ENV HTTP_PORT=9000
ENV HOST_BASEURL=""
ENV APPLICATION_SECRET="$(head -c 64 /dev/urandom | base64)"
ENV PLAY_CONFIG=$CONFIG_DIR/production.conf
ENV RD_RANDOM_DATA=-1


USER 999

VOLUME $CONFIG_DIR

EXPOSE 9000

ENTRYPOINT /opt/$BACKEND_APP/bin/dnpm-dip-api-gateway \
    -Dhttp.port="$HTTP_PORT" \
    -Dplay.http.secret.key="$APPLICATION_SECRET" \
    -Dconfig.file="$PLAY_CONFIG" \
    -Dpidfile.path="$PID_FILE" \
    -Ddnpm.dip.rest.api.baseurl="$HOST_BASEURL" \
    -Ddnpm.dip.rd.query.data.generate="$RD_RANDOM_DATA"

