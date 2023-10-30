# DNPM:DIP Backend - Docker


**Preliminary - Work In Progress**


## Docker Image Creation

The provided Dockerfile requires the self-contained zip package of the backend application, as created by the <code>sbt dist</code> task.
With this zip package named <code>dnpm-dip-api-gateway-{VERSION}.zip</code> in the current work directory:

```bash
foo@bar: docker build -t dnpm-dip-backend --build-arg="BACKEND_APP=dnpm-dip-api-gateway-{VERSION}" .
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
      # For random data generation: Set to a positive Int to have the application generate N random RD patient record
      RD_RANDOM_DATA: 50 

      # For HATEOAS: Base-URL under which the backend app is exposed to e.g. the frontend, so that full URLs can be set in the hypermedia
      HOST_BASEURL: "http://localhost:9000" 

    volumes:
      # External Volume containing config files (e.g. production.conf)
      - /PATH/TO/CONFIG_DIR:/config  


```

