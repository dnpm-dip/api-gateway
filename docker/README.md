# DNPM:DIP Backend - Docker


**Preliminary - Work In Progress**


## Docker Image Creation

The provided Dockerfile requires the zip package of the backend application, as created by the <code>sbt dist</code> task.
With this zip package named <code>dnpm-dip-api-gateway-{VERSION}.zip</code> in the current work directory:

```bash
foo@bar: docker build -t dnpm-dip-backend --build-arg BACKEND_APP=dnpm-dip-api-gateway-{VERSION} .
```


## Operation

Here's a sample docker-compose:

```yaml
version: '3.7'

services:
  backend:

    image: dnpm-dip-backend

    ports:
      - 9000:9000

    environment:
      # OPTIONAL: Set to a positive Int to have the application generate N random Rare Disease (RD) or Mol. Tumor Board (MTB) patient records
      RD_RANDOM_DATA: 50
      MTB_RANDOM_DATA: 50

      # OPTIONAL for HATEOAS: Base-URL under which the backend is exposed to e.g. the frontend, so that full URLs can be set in the hypermedia
      HOST_BASEURL: "http://localhost:9000"

      # OPTIONAL: Override the type of connector used for connecting to external peers: { broker, peer2peer }. Default: broker
      CONNECTOR_TYPE: "peer2peer"

    volumes:
      # External volume containing config files (e.g. production.conf, logback.xml, config.xml)
      - /PATH/TO/CONFIG_DIR:/dnpm_config

      # External volume for persistence of the application's data (patient records, etc)
      - /PATH/TO/DATA_DIR:/dnpm_data

```
