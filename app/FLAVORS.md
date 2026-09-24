# Product flavors (dev / prod)

GroovePlayer uses an `environment` flavor dimension with two product flavors for migration readiness.

| Flavor | Purpose | Default? |
|--------|---------|----------|
| `dev`  | Day-to-day debug / LAN backend / AdMob **test** IDs / optional HTTP cleartext | Yes (`isDefault = true`) |
| `prod` | Release-oriented; production API placeholder; AdMob from `local.properties` / CI secrets | No |

Variants are `{flavor}{BuildType}`, e.g. `devDebug`, `prodRelease`.

## Assemble / install commands

```bash
# Day-to-day (preferred)
./gradlew :app:assembleDevDebug
./gradlew :app:installDevDebug

# Production-shaped debug (debug signing; useful for QA without a release keystore)
./gradlew :app:assembleProdDebug
./gradlew :app:installProdDebug

# Release APK (currently signed with the **debug** keystore so local/CI compile works
# without a Play upload keystore — replace signingConfig before Play Store upload)
./gradlew :app:assembleProdRelease

# Also valid
./gradlew :app:assembleDevRelease
```

### What `./gradlew :app:assembleDebug` does now

With product flavors, `assembleDebug` builds **all** debug variants (`devDebug` **and** `prodDebug`). It still works; it is just broader than before.

Prefer `./gradlew :app:assembleDevDebug` (or Run `devDebug` in Android Studio — `dev` is marked `isDefault`).

`./gradlew :app:installDebug` is ambiguous with multiple flavors; use `installDevDebug` or `installProdDebug`.

## How config differs

### API base URL (`BuildConfig.API_BASE_URL`)

| Flavor | Source | Default |
|--------|--------|---------|
| `dev`  | `API_BASE_URL` in `local.properties` | `http://10.0.2.2:8080` (emulator → host) |
| `prod` | `PROD_API_BASE_URL` env **or** `local.properties` | `https://grooveplayer-backend.fly.dev` |

### AdMob

| Flavor | Behavior |
|--------|----------|
| `dev`  | Always Google **sample/test** app + unit IDs (ignores real IDs in `local.properties`) |
| `prod` | `ADMOB_APP_ID`, `ADMOB_BANNER_UNIT_ID`, `ADMOB_INTERSTITIAL_UNIT_ID` from **CI env first**, then `local.properties`. **No sample-ID fallback.** `assembleProdRelease` / `bundleProdRelease` **fails** if any ID is missing. |

Manifest placeholder `${admobAppId}` → `com.google.android.gms.ads.APPLICATION_ID`.

### Cleartext HTTP

| Flavor | `BuildConfig.ALLOW_CLEARTEXT` | `network_security_config` |
|--------|-------------------------------|---------------------------|
| `dev`  | `true`  | `src/dev/res/xml/…` allows cleartext for emulator / localhost / LAN IP |
| `prod` | `false` | `src/prod/res/xml/…` — cleartext **not** permitted |

If your LAN IP changes, update the `<domain>` list in `app/src/dev/res/xml/network_security_config.xml` and `API_BASE_URL` in `local.properties`.

### Google Sign-In client IDs

Shared in `defaultConfig` for both flavors (`GOOGLE_WEB_CLIENT_ID` / `GOOGLE_ANDROID_CLIENT_ID`). Same `applicationId` — no `.dev` suffix — so existing Google Cloud package + SHA-1 registration keeps working.

## local.properties / CI (gitignored — never commit secrets)

```properties
# dev
API_BASE_URL=http://192.168.x.x:8080

# prod API (defaults to Fly.io if unset)
PROD_API_BASE_URL=https://grooveplayer-backend.fly.dev

# prod AdMob — REQUIRED for assembleProdRelease / bundleProdRelease
ADMOB_APP_ID=ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy
ADMOB_BANNER_UNIT_ID=ca-app-pub-xxxxxxxxxxxxxxxx/zzzzzzzzzz
ADMOB_INTERSTITIAL_UNIT_ID=ca-app-pub-xxxxxxxxxxxxxxxx/wwwwwwwwww

# Play upload keystore — REQUIRED for assembleProdRelease / bundleProdRelease
# RELEASE_STORE_FILE is relative to the project root (this folder).
RELEASE_STORE_FILE=keystore/grooveplayer-upload.jks
RELEASE_STORE_PASSWORD=…
RELEASE_KEY_ALIAS=…
RELEASE_KEY_PASSWORD=…

GOOGLE_WEB_CLIENT_ID=…
GOOGLE_ANDROID_CLIENT_ID=…
```

CI can inject the same keys as environment variables (`PROD_API_BASE_URL`, `ADMOB_*`, `RELEASE_*`); env wins over `local.properties`.

## Signing note

- `*Debug` → debug keystore (unchanged).
- `prodRelease` / `devRelease` → `signingConfigs.release` when all four `RELEASE_*` keys are set.
- `assembleProdRelease` / `bundleProdRelease` **fails closed** if AdMob IDs or release signing are missing (no debug-keystore fallback, no Google sample AdMob IDs).

See also `app/ADS_SETUP.md`, `app/AUTH_SETUP.md`.
