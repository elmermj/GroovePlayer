# Firebase setup

## App Distribution

Over-the-air installs and updates (no USB) are documented in [DISTRIBUTE.md](DISTRIBUTE.md).

That checklist covers the Firebase Android app (`com.aethelsoft.grooveplayer`), `app/google-services.json`, the `testers` group, the App Distribution service account, and the GitHub Actions secrets the workflow needs.

`app/google-services.json` is also what Crashlytics needs once that SDK is enabled. The App Distribution upload uses the App ID and service account from GitHub secrets, not the JSON file, so you can add `google-services.json` when you have the real download from the Firebase console.
