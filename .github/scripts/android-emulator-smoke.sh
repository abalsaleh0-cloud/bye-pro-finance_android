#!/usr/bin/env bash
set -euo pipefail

adb wait-for-device
boot_ready=false
for attempt in $(seq 1 90); do
  boot_completed="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  package_service="$(adb shell service check package 2>/dev/null || true)"
  activity_service="$(adb shell service check activity 2>/dev/null || true)"
  if [[ "$boot_completed" == "1" ]] && grep -q "found" <<<"$package_service" && grep -q "found" <<<"$activity_service"; then
    boot_ready=true
    break
  fi
  sleep 5
done

if [[ "$boot_ready" != "true" ]]; then
  echo "::error::Android emulator services did not become ready within the timeout."
  adb shell getprop sys.boot_completed || true
  adb shell service check package || true
  adb shell service check activity || true
  exit 1
fi

adb shell cmd package list packages >/dev/null
gradle --no-daemon :app:connectedDebugAndroidTest
