#!/bin/bash

# Variables
KEYSTORE_DIR="keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/temp_keystore.jks"
KEY_ALIAS="temp_key"
KEY_PASSWORD="temp_password"
APK_INPUT="andro-cli.apk"
SIGNED_APK_OUTPUT="andro-cli-signed.apk"

# Ensure keystore directory exists
mkdir -p "$KEYSTORE_DIR"

# Generate a new keystore and key
keytool -genkeypair \
    -keystore "$KEYSTORE_FILE" \
    -storepass "$KEY_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 365 \
    -dname "CN=Temporary, OU=Dev, O=AndroCLI, L=City, S=State, C=US"

# Sign the APK
jarsigner -keystore "$KEYSTORE_FILE" \
    -storepass "$KEY_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    "$APK_INPUT" "$KEY_ALIAS"

# Verify the APK signature
jarsigner -verify "$APK_INPUT"

# Align the APK (optional, requires zipalign from Android SDK)
if command -v zipalign &> /dev/null; then
    zipalign -v 4 "$APK_INPUT" "$SIGNED_APK_OUTPUT"
    echo "Signed APK aligned and saved as $SIGNED_APK_OUTPUT"
else
    echo "zipalign not found. Signed APK saved as $APK_INPUT"
fi

# Clean up temporary keystore
rm -f "$KEYSTORE_FILE"

echo "APK signing process completed."