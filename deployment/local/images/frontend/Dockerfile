FROM gradle:7.2.0-jdk11 AS build
ADD . .
RUN gradle markdown:markdown-frontend-compose:jsBrowserDistribution

FROM socialengine/nginx-spa:latest AS server
COPY --from=build ./home/gradle/markdown/markdown-frontend-compose/build/distributions ./app
EXPOSE 80
