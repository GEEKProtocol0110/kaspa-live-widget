# Signed releases

The source includes a release pipeline. Signing is not activated until the owner supplies a persistent key. No signed public download is claimed until the first workflow succeeds.

## One-time owner setup

Use your own computer, with Java 17, Python 3, Bash and the GitHub CLI installed. Sign in with `gh auth login` using an account permitted to manage this repository’s Actions secrets. From the repository root, run:

```bash
bash scripts/configure-signing.sh
```

The script creates `.signing/kaspa-widget-release.jks` only if it does not exist, asks for a password without displaying it, and uploads the key and signing configuration as GitHub Actions secrets. An existing key is reused; do not replace it. Back up the keystore and password securely outside this checkout. Losing this key prevents compatible updates to existing release installations. A password entered for an existing keystore must match its existing password. The generated key uses the same store/key password.

Five secrets: `WIDGET_KEYSTORE_BASE64`, `WIDGET_STORE_PASSWORD`, `WIDGET_KEY_ALIAS`, `WIDGET_KEY_PASSWORD`, `WIDGET_SIGNING_CERT_SHA256`. The GitHub connection used to prepare this change cannot configure repository secrets; owner setup is required. Do not paste a password or keystore into a chat, issue or pull request.

## Publish after review and merge

On `main`, increment both `versionCode` and `versionName` for each update (this change uses code 3 / name 1.2.0). Run `./gradlew build`, then create and push a matching tag:

```bash
git tag v1.2.0
git push origin v1.2.0
```

The workflow requires the tagged commit to belong to main, verifies the tag matches `versionName`, runs the complete build/tests/lint, signs and checks the certificate against its pinned fingerprint. If a previous public stable APK exists, it checks the same certificate and a strictly increasing version code. Failed GitHub requests stop publication. It removes the runner’s keystore even on failure.

The published assets are `kaspa-live-widget.apk` and `SHA256SUMS`. After the first successful signed release, the permanent download is:

https://github.com/GEEKProtocol0110/kaspa-live-widget/releases/latest/download/kaspa-live-widget.apk

This URL is not a working download before that release. Version-specific releases remain available from the Releases page. The app’s Downloads button opens that page. Install a later release APK over the earlier release to preserve settings and widgets. CI debug APKs use a different app ID and are for testing; they do not upgrade a release app.

## Local release verification

Set all four signing environment variables (`WIDGET_KEYSTORE_PATH`, `WIDGET_STORE_PASSWORD`, `WIDGET_KEY_ALIAS`, `WIDGET_KEY_PASSWORD`) and run `./gradlew assembleRelease`. With none set, local/PR builds stay unsigned. Partial configuration fails at configuration time. Use Android SDK `apksigner verify --print-certs` to inspect a signed APK. The signing identity is owned by the maintainer; this repository does not contain a private signing key.

Android’s signing requirements: https://developer.android.com/studio/publish/app-signing
