#!/usr/bin/env bash
# Local run. In OpenShift the Dockerfile CMD is the same uvicorn line.
set -euo pipefail
cd "$(dirname "$0")"
# The venv lives outside the tree by default: scripts/check-forbidden-strings.sh walks every
# directory except node_modules/dist/target and site-packages trips it (PLAT-1933).
VENV="${VENV:-$HOME/.venvs/statements-api}"
if [ ! -x "$VENV/bin/uvicorn" ]; then
  python3.11 -m venv "$VENV"
  "$VENV/bin/pip" install -q -r requirements.txt
fi
exec "$VENV/bin/uvicorn" app.main:app --host 0.0.0.0 --port "${PORT:-4519}"
