FROM gradle:7.2.0-jdk11 AS build
ADD . .
RUN gradle markdown:markdown-backend:installDist

FROM openjdk:11 AS run
COPY --from=build ./home/gradle/markdown/markdown-backend/build/install/markdown-backend /usr/local/
EXPOSE 1234
ENTRYPOINT [ "./usr/local/bin/markdown-backend" ]
