#!/bin/bash


for app in ../target/universal/*.zip; do
  version=$(echo $app | grep -oP "\d{1}\.\d{1}(.\d{1})?(-SNAPSHOT)?")
  break
done

echo "Image tag? (default: latest)"
read tag

IMAGE=ghcr.io/dnpm-dip/api-gateway:${tag:-latest}

cp ../target/universal/dnpm-dip-api-gateway-$version.zip .
#cp ../target/universal/dnpm-dip-api-gateway-1.0.0.zip .

sudo docker build -t $IMAGE --build-arg BACKEND_APP=dnpm-dip-api-gateway-$version .
#sudo docker build -t $IMAGE --build-arg BACKEND_APP=dnpm-dip-api-gateway-1.0.0 .


echo "Push image to GHCR? (y / N)"
while true; do
  read push 
  case $push in
    y*) sudo docker push $IMAGE; break;;
    * ) exit;;
  esac
done

