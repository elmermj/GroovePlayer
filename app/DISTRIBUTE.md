# Distribute GroovePlayer to a phone (no USB)

This is the one-time setup for Firebase App Distribution, then how to ship a signed APK from GitHub Actions. After the first install, later CI builds update the app in place.

The workflow is [`.github/workflows/distribute-apk.yml`](../.github/workflows/distribute-apk.yml). It builds the **dev release** variant (`:app:assembleDevRelease`), signs it with the upload keystore, uploads that APK to Firebase App Distribution, and also stores it as the GitHub Actions artifact `grooveplayer-dev-release`.

`dev` and `prod` use the same application id, `com.aethelsoft.grooveplayer`. CI publishes **dev** so the test track does not need production AdMob ids. This repo does not read `ADMOB_*` yet. When a prod flavor starts requiring them, keep distributing `devRelease`.

Each run sets:

- `versionCode` = GitHub Actions run number (`github.run_number`)
- `versionName` = `1.0.<run number>`

Local builds leave those unset and stay on versionCode `1`, versionName `1.0`. Android only replaces an install when the new `versionCode` is higher and the APK is signed with the same upload key.

## 1. Firebase project and Android app

1. Open the [Firebase console](https://console.firebase.google.com/).
2. Create or open a project named **GroovePlayer**.
3. Add an Android app with package name `com.aethelsoft.grooveplayer`.
4. Download `google-services.json`.
5. Put it at `app/google-services.json` and commit it. The file is not in the repo until you add it. Do not invent a substitute. Committing this file is expected. It is also what Crashlytics needs once that SDK is turned on.

App Distribution itself does not read `google-services.json` during the upload. The workflow uses the App ID and a service account from GitHub secrets (steps 5 and 6).

The App ID is the `mobilesdk_app_id` field in `google-services.json`. It looks like `1:1234567890:android:abcdef`. The same value is under Project settings → Your apps → App ID. You will paste it into `FIREBASE_APP_ID`.

## 2. Enable App Distribution

In the Firebase console, open **App Distribution** and enable it for the GroovePlayer Android app if it is not already on.

## 3. Testers

Every upload invites **`matt.elmer24@gmail.com`**. The workflow passes that address as `testers` on the Firebase upload, and it still sends the group alias `testers`. Elmer gets the release even when that group is empty or does not list his address. You do not add that inbox in the Firebase console for CI to deliver a build.

Creating the `testers` group is an optional backup for extra people. If a run fails because that alias does not exist, create it once. Elmer’s email does not have to be a member.

1. In App Distribution, open **Testers & Groups**.
2. Create a group whose **alias** is `testers`. The display name can be anything.
3. Add anyone else who should receive builds through the group.
4. Save. Firebase emails an invite. Accept it on the phone (step 6).

To invite more addresses without editing the workflow, set the repository **variable** `FIREBASE_TESTERS` (Settings → Secrets and variables → Actions → Variables) to a comma-separated email list. CI still appends `matt.elmer24@gmail.com` if that list omits it. A manual **Run workflow** can set `testers` the same way. Leave both empty and the only invited address is Elmer’s.

## 4. Service account

The GitHub Action needs a Google Cloud service account that can upload releases. It must not be a personal user login.

1. In Google Cloud, select the Firebase project.
2. Enable the **Firebase App Distribution API** for that project.
3. IAM → Service Accounts → Create service account.
4. Grant the role **Firebase App Distribution Admin**.
5. Open the account → Keys → Add key → JSON. Download the JSON file.
6. That entire JSON file is the `FIREBASE_SERVICE_ACCOUNT` secret. Do not commit it.

## 5. GitHub Actions secrets

Repo → **Settings → Secrets and variables → Actions → New repository secret**.

Add these as secrets, not as variables. Variables are visible in the UI. Never paste the values into a commit, a log, or this file.

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64 of `keystore/grooveplayer-upload.jks` (see below) |
| `RELEASE_STORE_PASSWORD` | Keystore store password |
| `RELEASE_KEY_ALIAS` | Key alias inside that keystore |
| `RELEASE_KEY_PASSWORD` | Key password |
| `FIREBASE_APP_ID` | `mobilesdk_app_id` (`1:…:android:…`) |
| `FIREBASE_SERVICE_ACCOUNT` | Full service-account JSON from step 4 |

Encode the keystore on your Mac from the repo root. This copies the base64 string to the clipboard and does not print it:

```bash
base64 -i keystore/grooveplayer-upload.jks | pbcopy
```

On Linux:

```bash
base64 -w 0 keystore/grooveplayer-upload.jks
```

Paste the clipboard into `ANDROID_KEYSTORE_BASE64` only. The workflow decodes it to `keystore/grooveplayer-upload.jks` on the runner. CI also writes a gitignored `local.properties` with `RELEASE_STORE_FILE` and the three `RELEASE_*` password/alias secrets. Those files stay on the runner.

Back up the `.jks` and the three release passwords somewhere private (for example 1Password). The phone can only update in place while every build uses this same upload key.

`keystore/` and `local.properties` are gitignored. Do not commit the `.jks`, `local.properties`, or the service-account JSON.

## 6. Phone

1. Install **Firebase App Tester** from the Play Store.
2. After the first successful **Distribute APK** run, open the invite email sent to `matt.elmer24@gmail.com` and accept it on that phone. An invite from the optional `testers` group is the same kind of acceptance.
3. When Android asks, allow App Tester to install unknown apps.

## 7. Run the workflow

`main` is matured production only. Do not treat it as the OTA or tester target, and do not push tester builds there.

`test` is the testing branch. A merge or push to `test` that changes `app/**`, Gradle files, or this workflow runs **Distribute APK** and publishes the signed APK to Firebase App Tester. Pushes to `main` do not run this workflow.

Mobi and anyone else landing WIP or test builds should put those commits on `test`, not `main`.

You can also run it by hand: GitHub → **Actions → Distribute APK → Run workflow**. Pick the branch that should be built.

- Branch: `test`
- `notes`: optional. Leave this empty to publish the automatic changelog. If you type notes, that text is what testers see (the version and short SHA are still included).
- `groups`: leave as `testers` unless you created another alias
- `testers`: optional extra emails, comma-separated. `matt.elmer24@gmail.com` is always included.

When it finishes, open App Tester on the phone and install or update GroovePlayer. The run also keeps a copy of the APK under Actions artifacts, named `grooveplayer-dev-release`.

### Release notes

On a push to `test`, and on a manual run when `notes` is empty, the workflow writes `release-notes.txt` from the last 15 non-merge commits on the branch being built. Each subject is a bullet. The file starts with the version name (`1.0.<run number>`) and a short commit SHA. Notes are capped under about 2KB so App Tester stays readable.

On **Run workflow**, a non-empty `notes` value replaces that commit list. Version and SHA stay at the top.

## 8. First install vs later updates

The first App Distribution install may refuse to update an app that was installed from Android Studio. Debug builds are signed with the debug keystore, not `grooveplayer-upload.jks`. Uninstall that copy once, then install from App Tester.

After that, leave the App Distribution build installed. The next successful workflow run uses the same upload key and a higher `versionCode` (the next run number), so Android updates in place.

If Android says the package already exists at the same version, run the workflow again. `versionCode` only increases on a new run. Re-running the same run keeps the same number.

## What you do not need for this test track

- A USB cable, after the checklist above is done
- `ADMOB_*` values. Those would matter for a prod build. This pipeline builds `devRelease`
- A fake `google-services.json`. Add the real download, or leave it out until Crashlytics needs it. The upload still works from `FIREBASE_APP_ID` and `FIREBASE_SERVICE_ACCOUNT`
