# Releasing Nemi

## Choose the version and source

Set the version code to one more than the latest published code, regardless
of the source branch. Set the version name and code in
[`app/build.gradle.kts`](../app/build.gradle.kts).

- **Major and minor releases.** Update the version through a PR into
  `develop`, then merge `develop` into `main`. Tag the reviewed release commit
  on `main`.
- **Patch releases.** Patch only the latest published release line. For
  example, once `0.2.0` ships, publish fixes as `0.2.1`. Android treats the
  higher version code as newer. Branch from `main` for the first patch on
  that line. For further patches, branch from the previous patch tag. Bump
  the patch version, assign the next version code, and merge the fixes into
  the patch branch. Tag and release directly from that branch. After release,
  squash-merge it into `develop`, preserving any newer version name and the
  higher version code, then delete the patch branch.

Use the version name as the tag, such as `0.2.0`. Build from a clean checkout
of the tagged commit.

## Build and sign locally

Use the same RSA-3072 signing key for each release. Keep it in a
password-protected PKCS#12 keystore (`.p12`) outside the checkout.

The signing script uses the Android SDK and Build Tools selected by Gradle.

From the tagged checkout, run the signing script and enter passwords at its prompts.
Replace `KEYSTORE_FILE` with the path to your `.p12` file. Quote the path if
it contains spaces or shell metacharacters.

```sh
./scripts/sign-release.sh --keystore KEYSTORE_FILE --alias release
```

The alias is optional for a single-key keystore. Use `--output PATH` to
choose the signed APK destination. The default is `app/build/outputs/apk/release/app-release.apk`.
The script preserves the unsigned Gradle output, verifies the signature, and
prints the signer fingerprint and signed file size.

Check the signer SHA-256
fingerprint against your backed-up release key and inspect the APK for the
expected version, package `dev.sjaramillo.nemi`, launcher, and Health Connect
privacy alias. Test Steps
permission, live sync, and CSV export and restore on a physical phone in the
Owner profile, following [AGENTS.md](../AGENTS.md).

## Publish for F-Droid

Independently build the same tag with `./gradlew :app:assembleRelease` from a
clean checkout. Use the F-Droid build recipe for this
rebuild. From the signed build's checkout, compare the APKs using the path to
the independent checkout as `unsigned_checkout`.

```sh
unsigned_checkout=/path/to/unsigned-checkout
apksigcopier compare --unsigned \
  app/build/outputs/apk/release/app-release.apk \
  "$unsigned_checkout/app/build/outputs/apk/release/app-release-unsigned.apk"
```

Publish the verified signed APK as `Nemi-<version>.apk` on the GitHub Release
for the tag. In the F-Droid recipe, pin the release commit, use the public asset
URL in `Builds.binary`, and set `AllowedAPKSigningKeys` to the SHA-256 signing
certificate fingerprint. F-Droid builds the unsigned APK itself and uses the
public signed APK to verify reproducibility. See the
[F-Droid reproducible builds guide](https://f-droid.org/docs/Reproducible_Builds/).
