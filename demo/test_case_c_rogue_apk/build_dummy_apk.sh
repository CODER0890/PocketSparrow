#!/usr/bin/env bash
# Generates a dummy test APK archive containing AndroidManifest.xml for offline permission auditing tests
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_APK="$SCRIPT_DIR/dummy_banking_trojan.apk"

echo "Packaging dummy test fixture: $OUTPUT_APK"
cd "$SCRIPT_DIR"
zip -j "$OUTPUT_APK" AndroidManifest.xml

echo "[OK] Generated dummy APK fixture with dangerous permissions (RECEIVE_SMS + INTERNET + SYSTEM_ALERT_WINDOW)."
