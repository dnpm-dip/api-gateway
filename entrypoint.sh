#!/usr/bin/env bash

/opt/bin/dnpm-dip-api-gateway
  -Dhttp.port="$HTTP_PORT" \
  -Dplay.http.secret.key="$APPLICATION_SECRET" \
  -Dconfig.file="$CONFIG_DIR/production.conf" \
  -Dpidfile.path="$PID_FILE" \
  -Dlogger.file="$CONFIG_DIR/logback.xml" \
  -Ddnpm.dip.site="$LOCAL_SITE" \
  -Ddnpm.dip.config.file="$CONFIG_DIR/config.xml" \
  -Ddnpm.dip.authup.url="$AUTHUP_URL" \
  -Ddnpm.dip.rest.api.baseurl="$HATEOAS_HOST" \
  -Ddnpm.dip.connector.type="$CONNECTOR_TYPE" \
  -Ddnpm.dip.data.dir="$DATA_DIR" \
  -Ddnpm.dip.rd.query.data.generate="$RD_RANDOM_DATA" \
  -Ddnpm.dip.mtb.query.data.generate="$MTB_RANDOM_DATA"
