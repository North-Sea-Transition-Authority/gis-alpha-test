FROM eclipse-temurin:21-alpine
COPY ./build/libs/gis-alpha-test.jar app.jar

RUN apk update && apk upgrade && apk add curl && rm -rf /var/cache/apk/*

ENV TZONE="Europe/London"
RUN apk add --update tzdata \
&& echo "${TZONE}" > /etc/timezone \
&& ln -sf /usr/share/zoneinfo/${TZONE} /etc/localtime

RUN adduser -S app-user
USER app-user

ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
