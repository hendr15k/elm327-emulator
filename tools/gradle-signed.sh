#!/usr/bin/env bash
# Build script for the ELM327 emulator. Sources the signing identity from
# outside the repository and forwards every gradle argument.
set -euo pipefail

CREDENTIALS="${ELM327_CREDENTIALS:-/root/elm327-signing/credentials.env}"

if [[ ! -f "$CREDENTIALS" ]]; then
  echo "Missing signing credentials: $CREDENTIALS" >&2
  exit 1
fi

# shellcheck disable=SC1090
set -a
. "$CREDENTIALS"
set +a

cd "$(dirname "$0")/.."
exec ./gradlew --no-daemon "$@"
