# platform-services
#
# One entry point for the whole directory so nobody has to remember which services are Maven,
# which are npm and which are the two Python ones. Jenkins does not use this file; each service has
# its own Jenkinsfile. This is for laptops and for the estate smoke test.
#
# Assumptions (BUILD_LOG.md has the long version):
#   JAVA11 / JAVA17   JDK homes. Boot 2.7 services build on 11, entitlements-service needs 17.
#   MVN               Maven 3.9.9. ~/.m2/settings.xml may carry the Central mirror from phase 0.
#   nvm with 18.19.0  Node services. `.nvmrc` in each.
#   python3.11        Python services. Venvs live under ~/.venvs, NOT in the tree (PLAT-1933).
#
# Nothing here upgrades anything. If a target fails on a version complaint, read BUILD_LOG.md first.

SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c

JAVA11 ?= /usr/lib/jvm/java-11-openjdk-amd64
JAVA17 ?= /usr/lib/jvm/java-17-openjdk-amd64
# Phase 0 unpacked Maven 3.9.9 under /opt rather than apt's 3.6 (BUILD_LOG.md). Prefer PATH if set.
MVN_BIN ?= $(shell command -v mvn 2>/dev/null || echo /opt/apache-maven-3.9.9/bin/mvn)
MVN    ?= $(MVN_BIN) -q -B
NODE_VERSION := 18.19.0
NVM_SH := $(HOME)/.nvm/nvm.sh
VENVS  ?= $(HOME)/.venvs

JAVA11_SERVICES := bedrock-adapter beacon-notifications alerts-preferences-service txn-posting-service pii-vault-service audit-trail-service
JAVA17_SERVICES := entitlements-service
NODE_SERVICES   := bff-retail bff-business iris-orchestrator documents-service
PY_SERVICES     := statements-api exposure-calc

define with_node
	. $(NVM_SH) && nvm use $(NODE_VERSION) >/dev/null
endef

.PHONY: help install build test coverage up down smoke run-local stop-local clean fixtures common-starter

help:
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | sed 's/:.*## /\t/' | column -t -s $$'\t'

# ------------------------------------------------------------------------------------------------
install: fixtures common-starter ## dependencies for every service (npm ci, venvs, common-starter into ~/.m2)
	for s in $(NODE_SERVICES); do
	  echo "== npm ci $$s"; $(with_node); (cd services/$$s && npm ci --no-audit --no-fund --loglevel=error)
	done
	for s in $(PY_SERVICES); do
	  echo "== venv $$s"
	  [ -x $(VENVS)/$$s/bin/python ] || python3.11 -m venv $(VENVS)/$$s
	  $(VENVS)/$$s/bin/pip install -q -r services/$$s/requirements.txt
	done

fixtures: ## build @meridian/domain-fixtures and regenerate fixtures/meridian-fixtures.json
	$(with_node)
	cd libs/ts/domain-fixtures && npm ci --no-audit --no-fund --loglevel=error && npm run build
	cd $(CURDIR) && node scripts/export-fixtures.js

common-starter: ## install libs/java/common-starter into the local Maven repository
	cd libs/java/common-starter && JAVA_HOME=$(JAVA11) $(MVN) install

# ------------------------------------------------------------------------------------------------
build: common-starter ## compile and package everything, no tests
	for s in $(JAVA11_SERVICES); do echo "== mvn package $$s"; (cd services/$$s && JAVA_HOME=$(JAVA11) $(MVN) -DskipTests package); done
	for s in $(JAVA17_SERVICES); do echo "== mvn package $$s"; (cd services/$$s && JAVA_HOME=$(JAVA17) $(MVN) -DskipTests package); done
	for s in $(NODE_SERVICES); do echo "== npm run build $$s"; $(with_node); (cd services/$$s && npm run build --silent); done
	for s in $(PY_SERVICES); do echo "== import $$s"; (cd services/$$s && $(VENVS)/$$s/bin/python -c "import app.main"); done

test: ## mvn verify (JUnit + JaCoCo) for Java, lint + jest --coverage for Node. Python has none.
	for s in $(JAVA11_SERVICES); do echo "== mvn verify $$s"; (cd services/$$s && JAVA_HOME=$(JAVA11) $(MVN) verify); done
	for s in $(JAVA17_SERVICES); do echo "== mvn verify $$s"; (cd services/$$s && JAVA_HOME=$(JAVA17) $(MVN) verify); done
	for s in $(NODE_SERVICES); do echo "== lint+jest $$s"; $(with_node); (cd services/$$s && npm run lint --silent && npm run test:coverage --silent 2>&1 | grep -E '^(Tests|Lines)'); done
	echo "== statements-api: no test framework (CAB-2021-1188). exposure-calc: no tests."

coverage: ## JaCoCo aggregate across the Java services + regenerate COVERAGE.md from the reports
	JAVA_HOME=$(JAVA17) $(MVN) -f build/pom.xml -DskipTests -Dcheckstyle.skip verify
	python3 scripts/coverage-md.py > COVERAGE.md
	@echo "aggregate html: build/coverage-aggregate/target/site/jacoco-aggregate/index.html"
	@sed -n '1,40p' COVERAGE.md

# ------------------------------------------------------------------------------------------------
up: ## docker compose up the thirteen services (expects mock-external already up on its ports)
	docker compose -f docker-compose.services.yml up -d --build

down: ## docker compose down
	docker compose -f docker-compose.services.yml down --remove-orphans

run-local: ## start every service as a local process (no Docker), pids in var/
	scripts/run-local.sh start

stop-local: ## stop what run-local started
	scripts/run-local.sh stop

# mock-external/estate-up.sh starts each service with `make -C platform-services run-<name>` when it
# finds the target, and looks for the service at platform-services/<name>. Our layout is
# services/<name>; the symlinks at this level keep estate-up happy until PLAT-2706 lands.
# These run in the foreground on the port estate-up expects; run-local.sh is the daemonised form.
run-%:
	scripts/run-local.sh start $*
	tail -F var/log/$*.log

smoke: ## health on every port + the statement PDF path. Services must already be up.
	scripts/smoke.sh

clean:
	for s in $(JAVA11_SERVICES) $(JAVA17_SERVICES); do rm -rf services/$$s/target; done
	for s in $(NODE_SERVICES); do rm -rf services/$$s/dist services/$$s/coverage; done
	rm -rf build/coverage-aggregate/target libs/java/common-starter/target
