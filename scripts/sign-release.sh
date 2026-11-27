#!/usr/bin/env bash
set -euo pipefail

usage() { echo "Usage: $0 --keystore PATH [--alias NAME] [--output PATH]"; }
fail() { echo "Error: $*" >&2; exit 1; }

root=$(cd -- "$(dirname -- "$0")/.." && pwd)
unsigned="$root/app/build/outputs/apk/release/app-release-unsigned.apk"
output="$root/app/build/signed/app-release.apk"
keystore=''
alias_args=()
while (( $# )); do
    case "$1" in
        -h|--help) usage; exit 0 ;;
        --keystore|--alias|--output)
            [[ -n ${2:-} && $2 != --* ]] || fail "Missing value for $1"
            case "$1" in
                --keystore) keystore=$2 ;;
                --alias) alias_args=(--ks-key-alias "$2") ;;
                --output) output=$2 ;;
            esac
            shift 2 ;;
        *) usage >&2; fail "Unknown option" ;;
    esac
done

[[ -f "$keystore" && -r "$keystore" ]] || fail "Supply a readable keystore with --keystore"
tools="${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK}/build-tools/37.0.0"
[[ -x "$tools/zipalign" && -x "$tools/apksigner" ]] || fail "Install Android SDK Build Tools 37.0.0 in $ANDROID_HOME"

# Repackage this variant and consume its known output, even after earlier builds.
"$root/gradlew" -p "$root" :app:packageRelease --rerun :app:assembleRelease
[[ -s "$unsigned" ]] || fail "Expected unsigned release APK at $unsigned"
[[ ! -d "$output" && ! -L "$output" && ! "$output" -ef "$unsigned" && ! "$output" -ef "$keystore" ]] ||
    fail "Choose an output file distinct from the unsigned APK and keystore"
mkdir -p -- "$(dirname -- "$output")"
output_dir=$(cd -- "$(dirname -- "$output")" && pwd)
output="$output_dir/$(basename -- "$output")"
temp=$(mktemp -d "$output_dir/.nemi-sign.XXXXXXXX")
trap 'rm -rf -- "$temp"' EXIT
trap 'exit 1' HUP INT TERM

input=$unsigned
if ! "$tools/zipalign" -c -P 16 4 "$unsigned"; then
    input="$temp/aligned.apk"
    "$tools/zipalign" -P 16 4 "$unsigned" "$input"
fi
"$tools/apksigner" sign --ks "$keystore" "${alias_args[@]}" \
    --alignment-preserved true --v1-signing-enabled false --v4-signing-enabled false \
    --out "$temp/signed.apk" "$input"
"$tools/apksigner" verify --verbose --print-certs "$temp/signed.apk"
mv -f -- "$temp/signed.apk" "$output"
printf 'Signed APK: %s (%d bytes)\n' "$output" "$(wc -c < "$output")"
