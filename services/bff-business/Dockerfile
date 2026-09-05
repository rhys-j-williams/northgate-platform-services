# Multi stage. Builder has the toolchain, runtime has dist/ and production deps only.
# Node version must match .nvmrc; the pipeline checks (TOOL-1150).
FROM artifactory.meridian.internal/docker-redhat-remote/ubi9/nodejs-18:1-80 AS build
WORKDIR /opt/app-root/src
# Build context is platform-services/. domain-fixtures is a file: dependency in the monorepo so it has
# to be inside the context; the Jenkins build installs it from Verdaccio instead (see Jenkinsfile).
COPY --chown=1001:0 libs/ts/domain-fixtures /opt/app-root/libs/ts/domain-fixtures
COPY --chown=1001:0 services/bff-business/package.json services/bff-business/package-lock.json services/bff-business/.npmrc ./
RUN npm ci --no-audit --no-fund
COPY --chown=1001:0 services/bff-business/ .
RUN npm run build && npm prune --omit=dev

FROM artifactory.meridian.internal/docker-redhat-remote/ubi9/nodejs-18-minimal:1-80
ARG GIT_SHA=unknown
LABEL org.opencontainers.image.title="bff-business" \
      org.opencontainers.image.revision="${GIT_SHA}" \
      meridian.bank/team="business-digital"
ENV NODE_ENV=production PORT=4501 TZ=America/New_York
WORKDIR /opt/app-root/src
COPY --from=build --chown=1001:0 /opt/app-root/src/dist ./dist
COPY --from=build --chown=1001:0 /opt/app-root/src/node_modules ./node_modules
COPY --from=build --chown=1001:0 /opt/app-root/src/package.json ./

USER 1001
EXPOSE 4501
CMD ["node", "dist/main.js"]
