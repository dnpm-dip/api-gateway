# DNPM:DIP Backend - Docker

**Work In Progress**

## Docker Image Creation

The provided Dockerfile requires the zip package of the backend application, as created by the `sbt dist` task.
With this zip package named `dnpm-dip-api-gateway-{VERSION}.zip` in the current work directory:

```bash
foo@bar: docker build -t dnpm-dip-backend --build-arg BACKEND_APP=dnpm-dip-api-gateway-{VERSION} .
```

## Operation

See the [deployment repository](https://github.com/KohlbacherLab/dnpm-dip-deployment)

