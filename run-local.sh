#!/bin/bash

docker compose -f ./deployment/local/topologies/local.yml down
docker compose -f ./deployment/local/topologies/local.yml up --build --force-recreate
