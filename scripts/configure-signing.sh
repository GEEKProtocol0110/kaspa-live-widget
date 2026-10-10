#!/usr/bin/env bash
# Run on the owner's computer. Keeps the same key for every public release.
set -euo pipefail
repo=GEEKProtocol0110/kaspa-live-widget
for tool in gh keytool python3; do command -v "$tool" >/dev/null || { echo "Install $tool first"; exit 1; }; done
gh auth status
umask 077
signing_dir="${WIDGET_SIGNING_DIRECTORY:-$PWD/.signing}"
mkdir -p "$signing_dir"
keystore="$signing_dir/kaspa-widget-release.jks"
read -rsp 'Keystore password (at least 12 characters): ' WIDGET_STORE_PASSWORD; echo
if [ "${#WIDGET_STORE_PASSWORD}" -lt 12 ]; then echo 'Use at least 12 characters'; exit 1; fi
export WIDGET_STORE_PASSWORD
WIDGET_KEY_ALIAS=kaspa-widget
export WIDGET_KEY_ALIAS
if [ ! -e "$keystore" ]; then
  read -rsp 'Repeat password: ' confirmation; echo
  test "$confirmation" = "$WIDGET_STORE_PASSWORD" || { echo 'Passwords did not match'; exit 1; }
  keytool -genkeypair -keystore "$keystore" -storetype JKS -alias "$WIDGET_KEY_ALIAS" -keyalg RSA -keysize 4096 -validity 10000 -dname 'CN=Kaspa Live Widget, O=GEEK Protocol' -storepass:env WIDGET_STORE_PASSWORD -keypass:env WIDGET_STORE_PASSWORD
fi
# Explicitly reusing this alias and password; no replacement key is generated.
keytool -exportcert -keystore "$keystore" -alias "$WIDGET_KEY_ALIAS" -storepass:env WIDGET_STORE_PASSWORD -file "$signing_dir/release-cert.der" >/dev/null
python3 - "$keystore" <<'PY' | gh secret set WIDGET_KEYSTORE_BASE64 --repo "$repo"
import base64,sys
print(base64.b64encode(open(sys.argv[1], 'rb').read()).decode())
PY
printf '%s' "$WIDGET_STORE_PASSWORD" | gh secret set WIDGET_STORE_PASSWORD --repo "$repo"
printf '%s' "$WIDGET_STORE_PASSWORD" | gh secret set WIDGET_KEY_PASSWORD --repo "$repo"
printf '%s' "$WIDGET_KEY_ALIAS" | gh secret set WIDGET_KEY_ALIAS --repo "$repo"
python3 - "$signing_dir/release-cert.der" <<'PY' | gh secret set WIDGET_SIGNING_CERT_SHA256 --repo "$repo"
import hashlib,sys
print(hashlib.sha256(open(sys.argv[1], 'rb').read()).hexdigest())
PY
unset WIDGET_STORE_PASSWORD confirmation
printf 'Signing configured. Back up %s and its password securely before creating any release tag.\n' "$keystore"
