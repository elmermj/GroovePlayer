# AdMob setup (GroovePlayer)

Ads are shown **only** when the signed-in (or guest) privilege tier is `FREE`.
`BASIC` and `PREMIUM` suppress all ads. There is no Premium upsell UI.

## Flavors

| Flavor | AdMob IDs |
|--------|-----------|
| **dev** (default day-to-day) | Hardcoded Google **sample/test** IDs — safe for debug |
| **prod** | From `local.properties` / CI secrets (`ADMOB_*`); **no sample fallback** — `prodRelease` fails if unset |

Build with:

```bash
./gradlew :app:assembleDevDebug      # test ads
./gradlew :app:assembleProdRelease # production units from secrets
```

See `app/FLAVORS.md` for the full flavor matrix.

## Where to put production unit IDs

Set these in the project-root `local.properties` (gitignored) **or** as CI environment variables (env wins):

```properties
ADMOB_APP_ID=ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy
ADMOB_BANNER_UNIT_ID=ca-app-pub-xxxxxxxxxxxxxxxx/zzzzzzzzzz
ADMOB_INTERSTITIAL_UNIT_ID=ca-app-pub-xxxxxxxxxxxxxxxx/wwwwwwwwww
```

They are read by `app/build.gradle.kts` **for the `prod` flavor** into:

- `manifestPlaceholders["admobAppId"]` → AndroidManifest `APPLICATION_ID`
- `BuildConfig.ADMOB_BANNER_UNIT_ID` → `BannerAdSlot`
- `BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID` → startup interstitial (`StartupInterstitialHelper`)

The `dev` flavor **always** uses Google sample/test IDs regardless of these keys.

## Placement

- Banner: compose `BannerAdSlot` (wire into shell UI when ready; currently available for free-tier layouts)
- Startup video: interstitial on cold start, max **2 per calendar day** (`StartupAdQuotaStore`)

Sign-In must never depend on ads loading successfully.

## UMP consent (EEA/UK)

`AdsConsentHelper` requests User Messaging Platform consent from `MainActivity` **before** `MobileAds.initialize`. Ads load only when `ConsentInformation.canRequestAds()` is true.
