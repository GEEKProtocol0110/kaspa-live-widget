# Signed releases

The first signed public release, [v1.3.0](https://github.com/GEEKProtocol0110/kaspa-live-widget/releases/tag/v1.3.0), was published on October 10, 2026. Its [release workflow](https://github.com/GEEKProtocol0110/kaspa-live-widget/actions/runs/38076101995) passed build/tests/lint and verified the APK certificate against the pinned signing fingerprint. Both the APK and `SHA256SUMS` are available publicly.

The signing key and credentials are already configured in this repository's encrypted Actions secrets. Reuse the maintainer's backed-up key for future updates; do not generate a replacement.

## One-time owner setup

Use your own computer, with Java 17, Python 3, Bash and the GitHub CLI installed. Sign in with `gh auth login` using an account permitted to manage this repository’s Actions secrets. From the repository root, run:

```bash
bash scripts/configure-signing.sh
```

The script creates `.signing/kaspa-widget-release.jks` only if it does not exist, asks for a password without displaying it, and uploads the key and signing configuration as GitHub Actions secrets. An existing key is reused; do not replace it. Back up the keystore and password securely outside this checkout. Losing this key prevents compatible updates to existing release installations. A password entered for an existing keystore must match its existing password. The generated key uses the same store/key password.

Five secrets: `WIDGET_KEYSTORE_BASE64`, `WIDGET_STORE_PASSWORD`, `WIDGET_KEY_ALIAS`, `WIDGET_KEY_PASSWORD`, `WIDGET_SIGNING_CERT_SHA256`. The GitHub connection used to prepare this change cannot configure repository secrets; owner setup is required. Do not paste a password or keystore into a chat, issue or pull request.

## Publish after review and merge

The first public release is **1.3.0**, with `versionCode` **6**, built from commit `c3fab90709edf8d69b2f7510c6f2a68f3e69fd02`. Its [release notes](releases/v1.3.0.md) describe the app. The published APK SHA-256 is `c41cc81365246ec1f374b22b56a74f892d3bb46b3102547a615e0ef6dc1ab035`. The automated checks do not constitute device acceptance.

Keep the signing key and password securely backed up before publishing. Use current reviewed `main`; increment both `versionCode` and `versionName` for later app updates. If using GitHub CLI authentication with an HTTPS checkout, run `gh auth setup-git` once so Git can push using that account. Run `./gradlew build`, then create and push the matching tag. For example, after reviewing and merging version 1.3.1 with a version code greater than 6:

```bash
git tag v1.3.1
git push origin v1.3.1
```

The workflow requires the tagged commit to belong to main, verifies the tag matches `versionName`, runs the complete build/tests/lint, signs and checks the certificate against its pinned fingerprint. If a previous public stable APK exists, it checks the same certificate and a strictly increasing version code. Failed GitHub requests stop publication. It removes the runner’s keystore even on failure.

Check that the **Signed release** workflow succeeds and the release contains both `kaspa-live-widget.apk` and `SHA256SUMS`. A tag by itself is not a public APK release. Use the tag matching the new app version; do not recreate or move the published v1.3.0 tag. The permanent download is:

https://github.com/GEEKProtocol0110/kaspa-live-widget/releases/latest/download/kaspa-live-widget.apk

Version-specific releases remain available from the Releases page. The app’s Downloads button opens that page. Install a later release APK over the earlier release to preserve settings and widgets. CI debug APKs use a different app ID and are for testing; they do not upgrade a release app.

## Local release verification

Set all four signing environment variables (`WIDGET_KEYSTORE_PATH`, `WIDGET_STORE_PASSWORD`, `WIDGET_KEY_ALIAS`, `WIDGET_KEY_PASSWORD`) and run `./gradlew assembleRelease`. With none set, local/PR builds stay unsigned. Partial configuration fails at configuration time. Use Android SDK `apksigner verify --print-certs` to inspect a signed APK. The signing identity is owned by the maintainer; this repository does not contain a private signing key.

Android’s signing requirements: https://developer.android.com/studio/publish/app-signing
