# ADR 0004: Sideload release signing from CloudAgenticCoding

## Status

Accepted

## Context

CI should publish an installable Android APK. The owner’s other Android hobby apps in `TepMex/CloudAgenticCoding` share a committed sideload keystore so updates install over previous builds without Play Store distribution.

## Decision

1. Copy the shared `sideload.keystore` and `sideload-signing.properties` into `android/`.
2. Apply the same `sideload-signing.gradle.kts` helper used by CloudAgenticCoding projects.
3. Sign both debug and release build types with that key when present.
4. Verify the APK certificate SHA-256 matches `expected-sideload-cert-sha256.txt` in CI.
5. Upload the release APK as a GitHub Actions artifact and publish a GitHub Release on `master` / manual dispatch.

This key is for sideload/CI convenience only, not Play Store publication.

## Consequences

- APKs from this repo install over earlier sideload builds that used the same key.
- The keystore password lives in-repo (same trade-off as CloudAgenticCoding). Do not reuse for production Play signing.
