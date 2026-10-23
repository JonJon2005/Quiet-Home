#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
project_dir="$PWD"
sdk_path="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
build_tools="$sdk_path/build-tools/${BUILD_TOOLS_VERSION:-36.0.0}"
android_jar="$sdk_path/platforms/${ANDROID_PLATFORM:-android-36.1}/android.jar"
build_dir="$project_dir/build"
private_dir="$project_dir/.local"
mkdir -p "$private_dir"
chmod 700 "$private_dir"
if [ ! -f "$private_dir/signing-password" ]; then
 python3 -c 'import secrets; print(secrets.token_urlsafe(32))' > "$private_dir/signing-password"
 chmod 600 "$private_dir/signing-password"
fi
if [ ! -f "$private_dir/signing.p12" ]; then
 keytool -genkeypair -keystore "$private_dir/signing.p12" -storepass:file "$private_dir/signing-password" -alias quiet-home -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Quiet Home Local Build'
 chmod 600 "$private_dir/signing.p12"
fi
mkdir -p "$build_dir/classes" "$build_dir/dex"
"$build_tools/aapt2" link -o "$build_dir/base.apk" --manifest AndroidManifest.xml -I "$android_jar"
javac --release 8 -classpath "$android_jar" -d "$build_dir/classes" src/local/quiethome/*.java
jar cf "$build_dir/classes.jar" -C "$build_dir/classes" .
"$build_tools/d8" --lib "$android_jar" --min-api 26 --output "$build_dir/dex" "$build_dir/classes.jar"
cp "$build_dir/base.apk" "$build_dir/unsigned.apk"
(cd "$build_dir/dex" && zip -q "$build_dir/unsigned.apk" classes.dex)
"$build_tools/zipalign" -f 4 "$build_dir/unsigned.apk" "$build_dir/aligned.apk"
"$build_tools/apksigner" sign --ks "$private_dir/signing.p12" --ks-pass "file:$private_dir/signing-password" --out "$project_dir/quiet-home.apk" "$build_dir/aligned.apk"
"$build_tools/apksigner" verify "$project_dir/quiet-home.apk"
echo "Built $project_dir/quiet-home.apk"
