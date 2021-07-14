FROM nginx:1.21.0

LABEL maintainer="alexandre@piveteau.email"

# Adapted from https://github.com/rjoonas/heroku-docker-nginx-example/blob/master/Dockerfile
# ADD ./markdown-frontend/build/distributions /web
ADD ./markdown-frontend/build/distributions /usr/share/nginx/html
ADD ./infra/docker/frontend/default.conf.template /etc/nginx/conf.d/default.conf.template
ADD ./infra/docker/frontend/nginx.conf /etc/nginx/nginx.conf

CMD /bin/bash -c "envsubst '\$PORT' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf" && nginx -g 'daemon off;'
# CMD /bin/bash -c "envsubst '\$PORT' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf" && /bin/bash -c "ls /etc/nginx/conf.d/default.conf"
