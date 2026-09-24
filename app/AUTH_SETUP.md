# Google Sign-In + API setup

## local.properties (gitignored)

```properties
sdk.dir=...
# used by the `dev` flavor
API_BASE_URL=http://10.0.2.2:8080
# used by the `prod` flavor (placeholder until real host exists)
PROD_API_BASE_URL=https://api.grooveplayer.example
GOOGLE_WEB_CLIENT_ID=356328665268-h7p2ct3o1k8358itiecp3h6o4r1tjjgo.apps.googleusercontent.com
GOOGLE_ANDROID_CLIENT_ID=356328665268-1t80fc2j091cei383co7tncernl4p00c.apps.googleusercontent.com
```

- **Web client ID** is used as Credential Manager `serverClientId` (required so the ID token `aud` matches backend verify).
- **Android client ID** must stay registered in Google Cloud for package `com.aethelsoft.grooveplayer` + your debug/release SHA-1.
- Emulator → host backend (`dev`): `http://10.0.2.2:8080`. Physical device: use your Mac LAN IP, e.g. `http://192.168.x.x:8080`, and allow that host in `app/src/dev/res/xml/network_security_config.xml` if using HTTP.
- Prefer `./gradlew :app:assembleDevDebug` / `installDevDebug` for local auth testing. See `app/FLAVORS.md`.

## Test against grooveplayer-backend

```bash
cd "$HOME/Documents/Private Projects/grooveplayer-backend"
cp -n .env.example .env   # set JWT_SECRET
go run ./cmd/server
# listens on :8080
curl -s http://127.0.0.1:8080/healthz
```

App flow: Profile → Account → **Sign in with Google** → app POSTs `{ "id_token", "platform":"android" }` to `/v1/auth/google` → stores JWT pair in EncryptedSharedPreferences → Room `UserProfile` + tier.

## Tiers / ads

| Server tier | PrivilegeTier | Ads |
|-------------|---------------|-----|
| free / signed-out | FREE | Banner + startup interstitial (max 2/day) |
| basic | BASIC | Off |
| premium | PREMIUM | Off + cloud backup UI |

Billing / backup: see `app/BILLING_SETUP.md` and backend `docs/play-billing.md`.
AdMob IDs: see `app/ADS_SETUP.md`.


## Delete account (Play Store)

Profile → **Delete account** (signed-in only). Contract (Benny):

| | |
|---|---|
| Method / path | `DELETE /v1/account` |
| Auth | `Authorization: Bearer <access_token>` (OkHttp interceptor). Body: none. |
| Success | **200** `{"deleted":true}` or `{"deleted":true,"already_gone":true}` (idempotent). **Not 204.** |
| Errors | **401** bad/missing token · **500** R2 purge or DB delete failed mid-flight |
| R2 | Purge is synchronous before 200; on 500 do not assume purged |
| Billing | Backend wipes entitlement rows only — **does not** cancel Google Play subscriptions |

Client behavior:

1. Confirm dialog (warns user to cancel Play subs in Google Play first)
2. Call `DELETE /v1/account`
3. On **any 200**: clear tokens, Credential Manager session, local profile / cached `/v1/me`; treat as signed out; **do not** call refresh
4. On **401 / 500 / other**: **do not** clear session; show error and let the user retry

Live on `https://grooveplayer-backend.fly.dev` (Benny). Smoke with a signed-in test account from Profile → Delete account.
