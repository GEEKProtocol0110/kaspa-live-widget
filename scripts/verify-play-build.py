#!/usr/bin/env python3
"""Verify that the Play bundle is compiled for store-managed updates."""
from pathlib import Path
import xml.etree.ElementTree as ET

manifests = list(Path("app/build/intermediates/merged_manifests/release").rglob("AndroidManifest.xml"))
assert manifests, "No merged Play manifest found"
for path in manifests:
    permissions = [node.get("{http://schemas.android.com/apk/res/android}name")
                   for node in ET.parse(path).getroot().findall("uses-permission")]
    assert "android.permission.REQUEST_INSTALL_PACKAGES" not in permissions, "Play bundle must use Play updates"
configs = list(Path("app/build/generated/source/buildConfig/release").rglob("BuildConfig.java"))
assert configs and all("DIRECT_UPDATES = false" in path.read_text() for path in configs), "Play updater flag must be off"
print("Play build excludes the direct updater permission and disables direct downloads.")
