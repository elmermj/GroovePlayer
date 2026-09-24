# Play Billing + Backup setup

Contract: `grooveplayer-backend/docs/play-billing.md`.

## Product IDs (Play Console)

| product_id | kind | notes |
|---|---|---|
| `groove_basic_monthly` | subscription | Basic — no ads ($0.99 US) |
| `groove_premium_monthly` | subscription | Premium — 40 GB backup; regional PPP via Play |
| `groove_storage_20gb_monthly` | subscription (stackable, max 5) | +20 GB; renews on **same date** as Premium |

Do **not** invent T1/T2/T3 SKUs — region pricing is on the single Premium/addon SKU.

Package: `com.aethelsoft.grooveplayer`.

## Client constants

`BillingProductIds` in `utils/BillingProductIds.kt`.

## `/v1/me.storage` (live on :8080 — premium_period_end null / addons [] for free)

```json
{
  "quota_bytes": 0,
  "used_bytes": 0,
  "addon_count": 0,
  "usage_ratio": 0.0,
  "soft_warn": false,
  "hard_stop": false,
  "read_only": false,
  "grace_until": null,
  "premium_period_end": "2026-10-09T00:00:00Z",
  "addons": [
    { "id": "...", "product_id": "groove_storage_20gb_monthly", "pack_gb": 20, "status": "active", "expires_at": "2026-10-09T00:00:00Z" }
  ],
  "max_addon_packs": 5
}
```

- Soft warn ≥ 80% · hard stop ≥ 100% (block backup)
- Grace: `read_only` + countdown; Benny wipes R2 after grace
- UI shows **one** renewal date: `storage.premium_period_end` (addons[].expires_at always equals it)

## Purchase flow

1. `BillingClient.launchBillingFlow`
2. `POST /v1/billing/play/verify` `{ product_id, purchase_token, package_name? }` → `/v1/me`-shaped user
3. `BillingClient.acknowledgePurchase` (device)
4. `POST /v1/billing/play/ack` `{ product_id, purchase_token }`

Dry-run (no `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`): any non-empty token for a known product is treated active (~1 month).

Add-on without Premium → conflict (`storage add-on requires an active Premium subscription`).  
6th pack → conflict (`max addon packs (5) reached`).

## How to test

1. Start backend: `cd "$HOME/Documents/Private Projects/grooveplayer-backend" && go run ./cmd/server`
2. Sign in (Profile → Account)
3. Purchase Basic / Premium / +20 GB (license testers or dry-run verify after a Play sheet / restore)
4. Profile → Subscription quota + renewal; **Cloud backup** → Back up now
5. Cancel Premium on backend / Play → grace countdown + blocked backup
6. Ads: Free only (Basic/Premium hide AdMob)

## Backend gaps (Benny)

- Real Play Developer API verify when `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` is set

## R2 live client path (Tessa / Constantine)

Backend with `R2_DRY_RUN=0` + bucket `grooveplayer-backups` returns `dry_run: false` (or omits it → client default false).
Client then: `POST /v1/backup/upload-url` → real `PUT` to presigned URL → `POST /v1/backup/complete`.

Happy-path needs:
1. Signed-in **Premium** user (`/v1/me.storage` present, **not** optimistic stub)
2. `storage.read_only == false` (not Premium cancel grace, not over_quota)
3. Under quota / not `hard_stop` (`used_bytes < quota_bytes`)
4. Dev flavor pointed at live `:8080` (`API_BASE_URL` in `local.properties`)
5. Cleartext allowed for LAN HTTP (`dev` flavor + network security config)

Blocked UX (no crash):
- `read_only` / HTTP 403 → clear grace or Free-cloud-space message
- `over_quota` → **Free cloud space** trim (unchanged)
- `hard_stop` / HTTP 507 → storage full message
