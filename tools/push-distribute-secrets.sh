#!/usr/bin/env bash
# Push signing + (optional) Firebase secrets to GitHub Actions for elmermj/GroovePlayer.
# Usage: ./tools/push-distribute-secrets.sh
# Requires: gh auth login  (repo scope)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
CRED="$ROOT/keystore/grooveplayer-upload.credentials"
JKS="$ROOT/keystore/grooveplayer-upload.jks"
if [[ ! -f "$CRED" || ! -f "$JKS" ]]; then
  echo "Missing keystore or credentials file under keystore/"
  exit 1
fi
if ! gh auth status >/dev/null 2>&1; then
  echo "Run: gh auth login"
  exit 1
fi
# shellcheck disable=SC1090
set -a
# parse credentials without echoing
RELEASE_STORE_PASSWORD=$(grep '^RELEASE_STORE_PASSWORD=' "$CRED" | cut -d= -f2-)
RELEASE_KEY_ALIAS=$(grep '^RELEASE_KEY_ALIAS=' "$CRED" | cut -d= -f2-)
RELEASE_KEY_PASSWORD=$(grep '^RELEASE_KEY_PASSWORD=' "$CRED" | cut -d= -f2-)
set +a
B64=$(base64 < "$JKS" | tr -d '\n')
gh secret set ANDROID_KEYSTORE_BASE64 --body "$B64" -R elmermj/GroovePlayer
gh secret set RELEASE_STORE_PASSWORD --body "$RELEASE_STORE_PASSWORD" -R elmermj/GroovePlayer
gh secret set RELEASE_KEY_ALIAS --body "$RELEASE_KEY_ALIAS" -R elmermj/GroovePlayer
gh secret set RELEASE_KEY_PASSWORD --body "$RELEASE_KEY_PASSWORD" -R elmermj/GroovePlayer
echo "Signing secrets pushed."
if [[ -f "$ROOT/app/google-services.json" ]]; then
  python3 - <<'PY'
import json
import subprocess
from pathlib import Path

config = json.loads(Path("app/google-services.json").read_text())
wanted = {
    "com.aethelsoft.grooveplayer": "FIREBASE_APP_ID",
    "com.aethelsoft.grooveplayer.staging": "FIREBASE_APP_ID_STAGING",
}
found = {}
for client in config.get("client", []):
    info = client.get("client_info", {})
    package = info.get("android_client_info", {}).get("package_name", "")
    app_id = info.get("mobilesdk_app_id", "")
    if package in wanted and app_id:
        found[package] = app_id
for package, secret in wanted.items():
    app_id = found.get(package, "")
    if not app_id:
        print(f"Skip {secret} — no google-services client for {package}.")
        continue
    subprocess.run(
        ["gh", "secret", "set", secret, "--body", app_id, "-R", "elmermj/GroovePlayer"],
        check=True,
    )
    print(f"{secret} pushed from google-services.json ({package}).")
PY
else
  echo "Skip FIREBASE_APP_ID — add app/google-services.json first, then re-run."
fi
if [[ -f "$ROOT/keystore/firebase-service-account.json" ]]; then
  gh secret set FIREBASE_SERVICE_ACCOUNT < "$ROOT/keystore/firebase-service-account.json" -R elmermj/GroovePlayer
  echo "FIREBASE_SERVICE_ACCOUNT pushed."
else
  echo "Skip FIREBASE_SERVICE_ACCOUNT — save the JSON key to keystore/firebase-service-account.json (gitignored) and re-run."
fi
echo "Done."
