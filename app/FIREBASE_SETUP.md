# Firebase setup

## App Distribution

Over-the-air installs and updates (no USB) are documented in [DISTRIBUTE.md](DISTRIBUTE.md). Merging or pushing to `test` runs Distribute APK and publishes to Firebase App Tester. `main` is production; do not use it as the OTA target. Mobi and others should land WIP and test builds on `test`.

That checklist covers the Firebase Android app (`com.aethelsoft.grooveplayer`), `app/google-services.json`, the App Distribution service account, and the GitHub Actions secrets the workflow needs. CI invites `matt.elmer24@gmail.com` on every upload and also sends the `testers` group. Creating that group in the console is an optional backup, not the only way Elmer gets a build.

`app/google-services.json` is also what Crashlytics needs once that SDK is enabled. The App Distribution upload uses the App ID and service account from GitHub secrets, not the JSON file, so you can add `google-services.json` when you have the real download from the Firebase console.
