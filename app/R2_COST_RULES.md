# R2 cost rules (mandatory — Jorge / Elmer)

Cloudflare R2 Class A (writes / lists) and Class B (reads) are billed per request.
The Android client **must** obey these rules. Tessa / CI may fail builds that
introduce Range-GET spam or client-side S3 ListObjects.

Companion: [FLAVORS.md](./FLAVORS.md). Implementation: `BackupRepositoryImpl`,
`NetworkModule` (`r2HttpClient`), `ExoPlayerManager` (local URIs only).

---

## 1. Double-guard skip — never remint upload-url on skip

**Already shipping (do not regress).**

Per song, before any `POST /v1/backup/upload-url` or R2 PUT:

1. **Belt:** local set from `GET /v1/backup/objects` (Postgres catalog) keyed by
   `basename(logical_path) + size_bytes`.
2. **Preferred:** `POST /v1/backup/match` (server basename+size; no hash yet).

On either hit → count as skipped and **continue**. Do **not** call upload-url,
do **not** PUT, do **not** complete.

Hash short-circuit via `upload-url` `deduped=true` remains the third guard when
basename/path differed but content matches — that path may mint upload-url once
but must never PUT.

## 2. Streaming / download — one GetObject per song (whole object)

- R2 GET must fetch the **entire** object in **one** request.
- **No** `Range: bytes=…` / 1 MiB chunk spam from OkHttp, ExoPlayer, restore, or
  any downloader.
- Seek optimization with Range is **forbidden** until Elmer approves a design
  with a hard cap on Range requests per object.
- `r2HttpClient` strips any `Range` header and refuses `HEAD` (see rule 5).
- ExoPlayer plays **local** `content://` / `file://` URIs only. Never point
  Media3 at a signed R2 URL (Media3 would Range-GET by default).

## 3. Reuse download signed URLs in-session until near expiry

- Cache `download_url` + expiry (`expires_in_sec`) keyed by `content_hash` and/or
  `r2_key` for the process lifetime.
- Remint via `POST /v1/backup/download-url` only when:
  - cache miss, or
  - remaining TTL < skew (default **120s**).
- Do **not** remint on every pause, seek, retry of the same object, or UI redraw.

## 4. Never ListObjects from the mobile client

- **Forbidden:** AWS/S3 SDK `ListObjects` / `ListObjectsV2` / `listBuckets` /
  prefix listing / any direct R2 list API from the app.
- **Allowed catalog:** `GET /v1/backup/objects` (Postgres-backed Benny API only).
- There is no AWS SDK dependency in this module — keep it that way.

## 5. HeadObject only on complete (once)

- Client must **not** send HTTP `HEAD` to R2 (progress ticks, size probes, etc.).
- Server may `HeadObject` inside `POST /v1/backup/complete` **once** per upload
  to verify object presence/size (HTTP 422 → incomplete upload / Retry).
- `r2HttpClient` rejects `HEAD` to fail builds/runtime that regress this.

## 6. Room DB — upload only if changed (hash/size)

- Snapshot path: checkpoint WAL → gzip `room.db` → SHA-256.
- **Local skip:** if gzip hash+size match the last successful/deduped upload
  stored in prefs → skip upload-url and PUT entirely.
- **Server dedupe:** `upload-url` `deduped=true` → no PUT (unchanged object).
- Never blindly re-PUT an identical `library/room.db.gz`.

---

## Quick audit checklist (for Tessa)

| Check | Pass criteria |
|-------|----------------|
| Range GET | No `Range` header on R2 requests; interceptor strips/refuses |
| GetObject | One full GET per song/object download |
| Download URL | In-session cache; remint only near expiry / miss |
| ListObjects | No S3 list; catalog = `GET /v1/backup/objects` only |
| HeadObject | No client HEAD; complete-only server verify |
| Skip upload-url | basename+size / match skip never calls upload-url |
| room.db | Local hash skip + server dedupe; no identical re-PUT |

## Residual risks

- Future “stream from cloud” features must download-to-file (or get Elmer
  approval for capped Range) — never wire signed URLs into ExoPlayer directly.
- Prefs-based room.db skip is per-device; another device may still upload-url
  once and receive server `deduped=true` (no PUT) — acceptable.
