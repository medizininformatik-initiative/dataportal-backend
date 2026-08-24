FROM eclipse-temurin:25.0.4_7-jre-alpine@sha256:3137541deb3cac6626b5d9a4a2187bc0d6a34312f858bd2c67dd01e732e6b682

WORKDIR /opt/dataportal-backend

ARG VERSION=9.1.0
ENV APP_VERSION=${VERSION}
ENV DATABASE_HOST="dataportal-network"
ENV DATABASE_PORT=5432
ENV DATABASE_USER=postgres
ENV DATABASE_PASSWORD=password
ENV CERTIFICATE_PATH=/opt/dataportal-backend/certs
ENV TRUSTSTORE_PATH=/opt/dataportal-backend/truststore
ENV TRUSTSTORE_FILE=self-signed-truststore.jks

RUN mkdir logging && \
    mkdir -p $CERTIFICATE_PATH $TRUSTSTORE_PATH && \
    chown -R 10001:10001 /opt/dataportal-backend && \
    chown 10001:10001 $CERTIFICATE_PATH $TRUSTSTORE_PATH && \
    apk --no-cache add curl bash
USER 10001

HEALTHCHECK --interval=5s --start-period=10s CMD curl -s -f http://localhost:8090/api/v6/actuator/health || exit 1

COPY ./target/*.jar ./dataportal-backend.jar
COPY ontology ontology
COPY ./docker-entrypoint.sh /

ENTRYPOINT ["/bin/bash", "/docker-entrypoint.sh"]