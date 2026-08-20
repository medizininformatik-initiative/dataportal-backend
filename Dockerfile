FROM eclipse-temurin:25.0.3_9-jre-alpine@sha256:28db6fdf60e38945e43d840c0333aeaec66c15943070104f7586fd3c9d1665b0

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

HEALTHCHECK --interval=5s --start-period=10s CMD curl -s -f http://localhost:8090/api/v6/actuator/health || exit 1

COPY ./target/*.jar ./dataportal-backend.jar
COPY ontology ontology
COPY ./docker-entrypoint.sh /

ENTRYPOINT ["/bin/bash", "/docker-entrypoint.sh"]