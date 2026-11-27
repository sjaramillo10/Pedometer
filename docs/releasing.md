# Releasing Nemi

## Choose the version and source

Set the version code to one more than the latest published code, regardless
of the source branch. Set the version name and code in
[`app/build.gradle.kts`](../app/build.gradle.kts).

- **Major and minor releases.** Update the version through a PR into
  `develop`, then merge `develop` into `main`. Tag the reviewed release commit
  on `main`.
- **Patch releases.** Branch from `main` for the first patch on a release line.
  For further patches on that line, branch from the previous patch tag. Bump
  the patch version, assign the next version code, and merge the fixes into
  the patch branch. Tag and release directly from that branch. After release,
  squash-merge it into `develop`, preserving any newer version name and the
  higher version code, then delete the patch branch.

Use the version name as the tag, such as `0.2.0`. Build from a clean checkout
of the tagged commit.

## Set up local release signing

Place the existing release keystore at `.signing/release/nemi.p12`. Put its
signing settings in `.signing/release/signing.properties` using the keystore's
alias and password.

```properties
storeFile=nemi.p12
storeType=PKCS12
keyAlias=nemi-release
storePassword=<keystore password>
keyPassword=<keystore password>
```

## Build and verify

```sh
./gradlew :app:assembleRelease
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
```

Use `apksigner` from the Android SDK Build Tools. Check the signer SHA-256
fingerprint against your backed-up release key and inspect the APK for the
expected version, package `dev.sjaramillo.nemi`, launcher, and Health Connect
privacy alias. Remove the local working credentials after signing. Test Steps
permission, live sync, and CSV export and restore on a physical phone in the
Owner profile, following [AGENTS.md](../AGENTS.md).

## Publish for F-Droid

Independently build the same tag with `./gradlew :app:assembleRelease` from a
checkout without `.signing/release/`. Use the F-Droid build recipe for this
rebuild and verify the match with the signed APK.

```sh
apksigcopier compare --unsigned app-release.apk app-release-unsigned.apk
```

Publish the verified signed APK as `Nemi-<version>.apk` on the GitHub Release
for the tag. In the F-Droid recipe, pin the release commit, use the public asset
URL in `Builds.binary`, and set `AllowedAPKSigningKeys` to the SHA-256 signing
certificate fingerprint. F-Droid builds the unsigned APK itself and uses the
public signed APK to verify reproducibility. See the
[F-Droid reproducible builds guide](https://f-droid.org/docs/Reproducible_Builds/).
