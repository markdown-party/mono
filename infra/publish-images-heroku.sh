#!/bin/bash

docker tag markdown-party-backend registry.heroku.com/markdown-party/web
docker tag markdown-party-frontend registry.heroku.com/markdown-party-frontend/web

# Log in to GitHub package registry.
echo $HEROKU_TOKEN | docker login registry.heroku.com --username=_ --password-stdin

# Push the Docker images.
docker push registry.heroku.com/markdown-party/web
docker push registry.heroku.com/markdown-party-frontend/web
