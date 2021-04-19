FROM openjdk:11

LABEL maintainer="alexandre@piveteau.email"

ADD ./markdown-backend/build/install/markdown-backend /usr/local/

ENTRYPOINT [ "./usr/local/bin/markdown-backend" ]
