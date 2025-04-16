#!/bin/bash


echo "Image tag? (default: latest)"
read tag

IMAGE=ghcr.io/dnpm-dip/backend:${tag:-latest}

cp ../target/universal/dnpm-dip-api-gateway-1.0-SNAPSHOT.zip .

sudo docker build -t $IMAGE --build-arg BACKEND_APP=dnpm-dip-api-gateway-1.0-SNAPSHOT .


echo "Push image to GHCR? (y / N)"
while true; do
  read push 
  case $push in
    y*) sudo docker push $IMAGE; break;;
    * ) exit;;
  esac
done

