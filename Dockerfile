FROM eclipse-temurin:25-jre-alpine@sha256:bf9c91071c4f90afebb31d735f111735975d6fe2b668a82339f8204202203621

WORKDIR /opt/dataportal-backend

ARG VERSION=6.0.0
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

HEALTHCHECK --interval=5s --start-period=10s CMD curl -s -f http://localhost:8090/api/v5/actuator/health || exit 1

COPY ./target/*.jar ./dataportal-backend.jar
COPY ontology ontology
COPY ./docker-entrypoint.sh /

ENTRYPOINT ["/bin/bash", "/docker-entrypoint.sh"]