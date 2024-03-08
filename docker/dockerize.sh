#!/bin/bash

cp ../target/universal/dnpm-dip-api-gateway-1.0-SNAPSHOT.zip . && sudo docker build -t dnpm-dip-backend --build-arg BACKEND_APP=dnpm-dip-api-gateway-1.0-SNAPSHOT .
