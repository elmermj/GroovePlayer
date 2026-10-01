# GroovePlayer

Android music player. The only library is the app-private `groove-library` folder. The app does not list MediaStore and it does not scan device folders.

## Library

Songs enter only by a manual folder import. The picker is the Storage Access Framework tree (`ACTION_OPEN_DOCUMENT_TREE`). Each audio file is copied into `groove-library`, then checked for size and SHA-256. A hash already in the library is skipped. Import accepts `mp3`, `m4a`, `aac`, `flac`, `ogg`, `opus`, and `wav` (plus other `audio/*` types). The catalog lists those files, and `wma`, that are still in the folder. Hidden and scratch files are not listed.

After a copy succeeds, the app asks whether to delete the originals. **Keep originals** is the default: dismiss, back, or Keep deletes nothing. Delete is a separate choice and only removes originals whose copy is still in the library. That delete uses the system MediaStore delete request. MediaStore is not a library source.

Albums, artists, genres, and search are built from those private files and from edited metadata.

## Playlists

Playlists hold private-library songs. Tracks can be added, removed, and reordered. Import and export use M3U. An M3U line matches a library song by path or file name, then by SHA-256 when the bytes can be read. A miss is left missing. It is not filled with a different song.

Recommendations are the top library tracks (up to 10) from three equal signals: play count, how many songs that artist has, and the current catalog order.

## Backup and restore

Cloud backup is Premium. Uploads are songs that already live in the private library. A cloud object with the same SHA-256 and size is skipped. The Room snapshot is uploaded only after the song files finish.

A song that exists only in the cloud is not in the library list. Restore downloads it into `groove-library`; it shows up after that file is on disk. The cloud object list is used for Premium availability badges on songs you already have, not as a second library.

## Nearby and NFC share

Nearby Connections carries the files (the P2P share and receive actions). NFC only pairs the two devices: it exchanges session host, port, and token, and it does not carry the audio. On the tap flow the bytes then go over the Wi-Fi share socket. Android Beam push exists only below API 29.

Received bytes stay in `groove-library/.incoming` until the size and SHA-256 match the sender. Only then does the file move to the library root, where the catalog can see it. A hash already in the private library is deleted and not added again. A failed check deletes the staged file.

## Wear OS remote

The `wear` module is a remote for the player on the paired phone. It does not play audio, store music, or call the backend. Controls are album art as the full-screen background, a progress ring along the screen edge (drag or tap to seek), swipe left for next, swipe right for previous, and a center tap for play or pause. When the phone is not reachable the screen shows Disconnected and the controls do nothing.

Install the same variant on the phone and the watch so the application id matches: `com.aethelworks.grooveplayer`, or `com.aethelworks.grooveplayer.staging` for the `staging` flavor and for a `dev` build while `GROOVE_ENV=staging`. Debug builds of both modules use the default debug keystore (`~/.android/debug.keystore`). Release builds of both use `RELEASE_STORE_*` from `local.properties` or CI. The Wearable Data Layer connects the two apps when that id and signing key match.

The phone advertises `grooveplayer_phone`. The watch advertises `grooveplayer_watch`. The watch is connected when a reachable node has `grooveplayer_phone`. The phone writes `/grooveplayer/state` (`title`, `artist`, `isPlaying`, `positionMs`, `durationMs`, `updatedAtMs`, and an optional JPEG `artwork` asset, longest edge 400px, only from a file already in the private library). The watch sends `/grooveplayer/cmd/play_pause`, `/grooveplayer/cmd/next`, `/grooveplayer/cmd/previous`, `/grooveplayer/cmd/seek` (8-byte big-endian `positionMs`), and `/grooveplayer/cmd/request_state` on launch and when the phone becomes reachable again. The watch moves the playhead locally while `isPlaying` is true.

`build-prod.sh`, `build-staging.sh`, and Distribute APK build and upload the phone APK only. The watch is not embedded in that APK.

```bash
./gradlew :wear:assembleDevDebug
./gradlew :wear:testDevDebugUnitTest
```

Android Studio previews, without a watch or an emulator: open `wear/src/main/java/com/aethelworks/grooveplayer/wear/ui/RemotePreviews.kt`, choose the wear **devDebug** variant, and use the Split or Design view. The previews are small round, large round, square, and rectangular, for playing, paused, and disconnected.

## Release lanes

| Branch | What ships | API |
| --- | --- | --- |
| `test` | App Tester build of GroovePlayer (`dev` release) | `https://grooveplayer-backend-test.fly.dev` |
| `staging-test` | App Tester build of GroovePlayer Staging (`.staging`, side by side) | `https://grooveplayer-backend-staging.fly.dev` |
| `main` | Production (`prod` flavor) | `https://grooveplayer-backend.fly.dev` |

A push to `test` or `staging-test` that touches the app or Gradle files runs **Distribute APK**. That workflow builds `app/build/outputs/apk/dev/release/app-dev-release.apk` or `app/build/outputs/apk/staging/release/app-staging-release.apk` and publishes it to Firebase App Distribution. It does not read `app/debug/` or `app/release/`. Version name is `1.0.<GitHub run number>` and versionCode is that run number. CI ignores `GROOVE_ENV` and the version keys in `local.properties`.

`build-prod.sh` and `build-staging.sh` point the default **devDebug** variant at production or GroovePlayer Staging. Each updates `API_BASE_URL`, `GROOVE_ENV`, `VERSION_NAME`, and `VERSION_CODE` in gitignored `local.properties`, leaves every other key alone, and builds `app/build/outputs/apk/dev/debug/app-dev-debug.apk`. A commit Distribute already published uses that App Tester version (`1.0.<run number>` and the same versionCode). Otherwise the version is `1.0.<latest run on this branch>-local`, plus `-dirty` when the tree has uncommitted changes. `--install` runs `adb install -r`. Gradle Sync in Android Studio after either script so Run and Debug follow that setup. The checked-in `google-services.json` still lists the previous package, so Google Services and Crashlytics stay off until that file includes `com.aethelworks.grooveplayer` and `com.aethelworks.grooveplayer.staging`. After the production package is listed, a local staging debug build can still derive a client until the staging package is in the file. The CI staging flavor still needs the real Firebase client.

Recent Updates in Profile is the asset `app/src/main/assets/recent_updates.json`.

Layouts are developed mainly on a large tablet. Phone layouts differ.

## Build and run

- JDK 17
- Android SDK 36
- minSdk 24 (phone), minSdk 30 (watch)
- compileSdk / targetSdk 36

```bash
./gradlew assembleDevDebug
./gradlew :app:installDevDebug
./gradlew testDevDebugUnitTest
```

`:app:assembleDevDebug` is the day-to-day phone build. `assembleStagingRelease` is the App Tester staging package. `assembleProdRelease` is the production-shaped release and needs the release keystore plus production AdMob ids. It does not fall back to the debug keystore.

## License

**Copyright (c) 2025-2026 Elmer Matthew Japara. All Rights Reserved.**

This project is proprietary software protected by copyright law under the Berne Convention and international copyright treaties.

Unauthorized copying, modification, distribution, or use of this software, via any medium, is strictly prohibited without the express written permission of the copyright holder.

## Author

**Elmer Matthew Japara**

Built with using Kotlin and Jetpack Compose

---

*"The main challenge is designing the UI. Once the design is done and all the features necessary implemented, it would be a cake walk doing this. The design process is without Figma, everything is done on top of my head."*
