#!/bin/bash

# Build with Gradle.
./gradlew markdown-backend:installDist markdown-frontend:browserDistribution

# Package in some Docker image.
docker build . -t markdown-party-backend -f ./infra/docker/backend/Dockerfile
docker build . -t markdown-party-frontend -f ./infra/docker/frontend/Dockerfile
