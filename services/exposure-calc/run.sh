#!/usr/bin/env bash
# How the desk runs it. There is no Dockerfile and no Jenkinsfile on purpose - see README.
set -euo pipefail
cd "$(dirname "$0")"
VENV="${VENV:-$HOME/.venvs/exposure-calc}"
if [ ! -x "$VENV/bin/uvicorn" ]; then
  python3.11 -m venv "$VENV"
  "$VENV/bin/pip" install -q -r requirements.txt
fi
exec "$VENV/bin/uvicorn" app.main:app --host 0.0.0.0 --port "${PORT:-4520}"
