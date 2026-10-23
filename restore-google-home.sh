#!/bin/sh
set -eu
run_adb() {
 if [ -n "${ADB_SERIAL:-}" ]; then adb -s "$ADB_SERIAL" "$@"; else adb "$@"; fi
}
run_adb shell pm default-state --user 0 com.google.android.apps.tv.launcherx
run_adb shell pm default-state --user 0 com.google.android.tungsten.setupwraith
run_adb shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.VanillaModeHomeActivity
run_adb shell input keyevent 3
