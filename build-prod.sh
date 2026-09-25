#!/usr/bin/env bash
# Point the default devDebug variant at the normal GroovePlayer package and the live prod API.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

INSTALL=0
for arg in "$@"; do
  case "$arg" in
    --install) INSTALL=1 ;;
    *)
      echo "Unknown argument: $arg" >&2
      echo "Usage: $(basename "$0") [--install]" >&2
      exit 1
      ;;
  esac
done

python3 - "$ROOT/local.properties" prod "https://grooveplayer-backend.fly.dev" << 'PY'
import sys
from pathlib import Path

path = Path(sys.argv[1])
env_name = sys.argv[2]
api_url = sys.argv[3]
updates = {
    "API_BASE_URL": api_url,
    "GROOVE_ENV": env_name,
}
original = path.read_text(encoding="utf-8") if path.exists() else ""
seen = set()
out = []
for line in original.splitlines():
    stripped = line.strip()
    if not stripped or stripped.startswith("#") or "=" not in stripped:
        out.append(line)
        continue
    key = stripped.split("=", 1)[0].strip()
    if key not in updates:
        out.append(line)
        continue
    if key in seen:
        continue
    out.append(f"{key}={updates[key]}")
    seen.add(key)
for key in ("API_BASE_URL", "GROOVE_ENV"):
    if key not in seen:
        out.append(f"{key}={updates[key]}")
text = "\n".join(out)
if text:
    text += "\n"
path.write_text(text, encoding="utf-8")
print(f"local.properties GROOVE_ENV={env_name} API_BASE_URL={api_url}")
PY

if [[ -z "${JAVA_HOME:-}" ]]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi
if [[ ! -x "${JAVA_HOME}/bin/java" ]]; then
  echo "Set JAVA_HOME, or install Android Studio at /Applications/Android Studio.app" >&2
  exit 1
fi

./gradlew :app:assembleDevDebug

APK="$ROOT/app/build/outputs/apk/dev/debug/app-dev-debug.apk"
if [[ ! -f "$APK" ]]; then
  echo "Debug APK not found at $APK" >&2
  exit 1
fi

if [[ "$INSTALL" -eq 1 ]]; then
  adb install -r "$APK"
fi

echo "APK: $APK"
echo "Do a Gradle Sync in Android Studio before Run or Debug."
