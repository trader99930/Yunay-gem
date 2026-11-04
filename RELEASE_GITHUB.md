# Building Release APKs with GitHub Actions

This repository ships a GitHub Actions workflow that builds **both** AntiGem
flavors and publishes signed APKs as a GitHub Release.

- Workflow: `.github/workflows/release.yml`
- Build script: `scripts/build_all_apks.sh --release`
- Outputs:
  - `antiGem-standard-v<version>-release.apk` — package `com.antigem` (Ubuntu PRoot container)
  - `antiGem-termux-v<version>-release.apk` — package `com.termux` (standalone Termux rootfs)
  - `agy_ide_bridge-v<version>-android-arm64` — optional standalone bridge binary
  - `SHA256SUMS.txt`

Both APKs are self-contained. The `standard` flavor does **not** require the
official Termux app, and the `termux` flavor does **not** require an external
bridge.

---

## 1. Enable Actions on your fork

Open your fork's **Actions** tab:

```text
https://github.com/<your-user>/<your-repo>/actions
```

If GitHub says workflows are disabled for the fork, click:

```text
I understand my workflows, go ahead and enable them
```

The workflow is called **Build & Publish Release**.

---

## 2. Create the release keystore

The release signing config in `app/build.gradle.kts` expects:

| Setting | Value |
| --- | --- |
| Key alias | `antigem` |
| Store password | `123456` |
| Key password | `123456` |
| Keystore file | `app/release.jks` (restored by CI) |

If you already have a release keystore, use that one. Otherwise create a new
keystore:

```bash
keytool -genkeypair -v \
  -keystore release.jks \
  -alias antigem \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass 123456 -keypass 123456 \
  -dname "CN=AntiGem, OU=Android, O=AntiGem, L=India, ST=India, C=IN"
```

Convert it to a single-line Base64 string:

```bash
base64 -w0 release.jks > release.jks.b64
```

Keep `release.jks` and `release.jks.b64` private. Never commit them.

> A new keystore means a new app signing identity. Use the same keystore for
> every future release, otherwise Android will reject updates with a signature
> mismatch.

---

## 3. Add the keystore secret

In GitHub:

```text
Repo -> Settings -> Secrets and variables -> Actions -> New repository secret
```

Create:

- **Name:** `RELEASE_KEYSTORE_BASE64`
- **Value:** the complete contents of `release.jks.b64`

The workflow fails early with a clear error if this secret is missing.

---

## 4. Trigger the release build

### Option A: Manual run

1. Open the **Actions** tab.
2. Select **Build & Publish Release**.
3. Click **Run workflow**.
4. Choose the `master` branch and run it.

### Option B: Push a version bump

The workflow also runs automatically when `version.json` changes on `master`.

Example:

```json
{
  "version_code": 115,
  "version_name": "1.0.15",
  "min_supported_version_code": 1,
  "changelog": [
    "Describe your changes here"
  ],
  "is_critical": false
}
```

Then commit and push that change to `master`.

---

## 5. Download the APKs

After the workflow finishes:

- The **Releases** page contains both APKs, the bridge binary and
  `SHA256SUMS.txt`.
- The workflow run's **Artifacts** section contains the same files, even if
  release publishing fails.

Verify the downloads:

```bash
sha256sum antiGem-standard-v<version>-release.apk
sha256sum antiGem-termux-v<version>-release.apk
```

---

## 6. Install and first run

### `standard` flavor (`com.antigem`)

1. Install `antiGem-standard-v<version>-release.apk`.
2. Open the app and allow notifications/storage permissions.
3. On the setup screen choose **Ubuntu ARM64 Base (Light)** (~29 MB) and tap
   **Download & Install**.
4. Wait for the container to download, extract and verify.
5. Pick the agent(s) you want: **Antigravity (agy)** and/or **Claude Code**.
6. Install/sign in to the selected agent from **Settings** when prompted.
7. Open a project from the sidebar or start a chat.

This flavor can be installed next to the official Termux app and next to the
`termux` flavor.

### `termux` flavor (`com.termux`)

1. Uninstall the official Termux app and any older AntiGem `termux` debug build
   first (same package id, different signature).
2. Install `antiGem-termux-v<version>-release.apk`.
3. Open the app and allow notifications/storage permissions.
4. On the setup screen keep **Auto Download from GitHub** (~350 MB) and tap
   **Download & Install**.
5. Wait for the Termux rootfs to download, extract and verify.
6. Pick the agent(s) you want and finish the agent sign-in flow.
7. Open the terminal or a project and start working.

---

## Troubleshooting

| Problem | Fix |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64 secret is missing` | Add the secret from step 3. |
| `go.mod requires go >= 1.26.5` | The workflow already pins Go `1.26.5`; do not lower it. |
| `Keystore was tampered with, or password was incorrect` | Regenerate the keystore with the alias/passwords shown above, or update the signing config. |
| Release tag already exists | Bump `version.json`, or re-run the workflow; the release assets are replaced. |
| R8/ProGuard build failure | Check the failing rule in `app/proguard-rules.pro` and the workflow log. |
| `App not installed` on the phone | Uninstall the older build of the **same package id** first (debug vs release signatures differ). |
| `standard` and `termux` both installed? | Yes, they have different package ids (`com.antigem` vs `com.termux`) and different ports, so they can run side by side. |
| Official Termux + AntiGem `termux` flavor | Not possible: both use `com.termux`. Install the `standard` flavor instead, or uninstall official Termux. |

---

## Notes

- Release builds are produced on GitHub's x86_64 runners with the official
  Android SDK, so the arm64/aapt2 workarounds used for local phone builds are
  not needed.
- The `termux` APK uses package id `com.termux`. It cannot coexist with the
  official Termux app (same package, different signature).
- The `standard` APK uses package id `com.antigem`. It can coexist with the
  official Termux app and with the `termux` flavor.
- Both flavors default to different localhost ports, so running both at the same
  time does not cause port conflicts:
  - `termux`: bridge `1234`, hub `1235`, browser MCP `8765`
  - `standard`: bridge `1236`, hub `1237`, browser MCP `8767`
- Do not build release locally; use this workflow.
