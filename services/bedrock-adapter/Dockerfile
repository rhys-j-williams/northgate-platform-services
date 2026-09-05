# Thin per-service layer over the platform JDK image. The heavy lifting (base image, user, entrypoint,
# JAVA_OPTS) is in platform-tooling/docker/java/Dockerfile and the Jenkins library builds that with
# --build-arg SERVICE=bedrock-adapter JDK_VERSION=11. This file exists so `docker compose` can build the
# service standalone for the estate smoke test and so CODEOWNERS has something to point GIS at.
ARG BASE=artifactory.meridian.internal/docker-redhat-remote/ubi9/openjdk-11-runtime:1.20
FROM ${BASE}

ARG GIT_SHA=unknown
LABEL org.opencontainers.image.title="bedrock-adapter" \
      org.opencontainers.image.revision="${GIT_SHA}" \
      meridian.bank/team="payments-platform" \
      meridian.bank/compliance-critical="true"

ENV JAVA_OPTS_APPEND="-Djava.security.egd=file:/dev/./urandom -Dlog4j2.formatMsgNoLookups=true" \
    SERVER_PORT=4516 \
    TZ=America/New_York

USER 185
WORKDIR /deployments
# Build context is platform-services/ (compose and the Jenkins library both do this).
COPY --chown=185:0 services/bedrock-adapter/target/bedrock-adapter-*.jar /deployments/app.jar

EXPOSE 4516
# The UBI openjdk image's own entrypoint runs /deployments/app.jar. Do not add a CMD.
