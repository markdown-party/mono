#!/bin/bash

# Build with Gradle.
./gradlew markdown:markdown-backend:installDist

# Package in a Docker image.
docker build -t markdown-party-backend .
