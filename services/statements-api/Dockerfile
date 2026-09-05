# statements-api has no place in the Java or Node pipeline, so this is the whole build.
# python-311 UBI image is on the GIS approved list as of GIS-STD-021 rev 4.
FROM artifactory.meridian.internal/docker-redhat-remote/ubi9/python-311:1-60
ARG GIT_SHA=unknown
LABEL org.opencontainers.image.title="statements-api" \
      org.opencontainers.image.revision="${GIT_SHA}" \
      meridian.bank/team="payments-platform"
WORKDIR /opt/app-root/src
# Build context is platform-services/ so the shared fixture export is reachable.
COPY --chown=1001:0 services/statements-api/requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY --chown=1001:0 services/statements-api/app ./app
COPY --chown=1001:0 fixtures/meridian-fixtures.json /opt/app-root/fixtures/meridian-fixtures.json
ENV MERIDIAN_FIXTURES=/opt/app-root/fixtures/meridian-fixtures.json PORT=4519
EXPOSE 4519
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "4519", "--workers", "2"]
