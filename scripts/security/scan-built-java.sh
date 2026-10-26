#!/usr/bin/env bash
set -euo pipefail
umask 077
artifact_root=${1:-build/libs}
[[ -d "$artifact_root" ]]
find "$artifact_root" -maxdepth 1 -type f -name '*.jar' -print -quit | grep -q .
java_scan_temp=$(mktemp -d)
trap 'rm -rf -- "$java_scan_temp"' EXIT
curl --fail --silent --show-error --location --retry 3 --proto '=https' --tlsv1.2 \
  'https://github.com/aquasecurity/trivy/releases/download/v0.76.1/trivy_0.76.1_Linux-64bit.tar.gz' \
  -o "$java_scan_temp/trivy.tar.gz"
printf '%s  %s\n' 'c7cbe3137fb8a48a17574bede3dd6beb16742d1e9772929940fc90082c39d9c0' "$java_scan_temp/trivy.tar.gz" | sha256sum --check --status
tar -xzf "$java_scan_temp/trivy.tar.gz" -C "$java_scan_temp" trivy
java_scan_status=0
# rootfs reads built Java archives; fs deliberately scans source manifests only.
# --include-dev-deps is not supported by rootfs: these are the actual runtime JAR contents.
"$java_scan_temp/trivy" rootfs --config='' --ignorefile='' --scanners vuln --pkg-types library \
  --severity HIGH,CRITICAL --exit-code 1 --offline-scan --disable-telemetry --skip-version-check \
  --timeout 10m --no-progress --format json --output "$java_scan_temp/report.json" "$artifact_root" \
  > "$java_scan_temp/scanner.log" 2>&1 || java_scan_status=$?
python3 scripts/security/check-built-java.py "$java_scan_temp/report.json" "$java_scan_status"
[[ -n ${RUNNER_TEMP:-} ]]
for configuration in runtimeClasspath testRuntimeClasspath; do
  mkdir -p "$java_scan_temp/resolved/$configuration"
  SECURITY_LOCK_DIR="$java_scan_temp/resolved/$configuration" bash gradlew --no-daemon \
    --init-script scripts/security/resolved-gradle.init.gradle \
    dependencies --configuration "$configuration" --write-locks \
    > "$java_scan_temp/gradle-$configuration.log" 2>&1
done
resolved_status=0
"$java_scan_temp/trivy" fs --config='' --ignorefile='' --scanners vuln --pkg-types library \
  --include-dev-deps --severity HIGH,CRITICAL --exit-code 1 --offline-scan \
  --disable-telemetry --skip-version-check --timeout 10m --no-progress --format json \
  --output "$java_scan_temp/resolved.json" "$java_scan_temp/resolved" \
  > "$java_scan_temp/resolved-scanner.log" 2>&1 || resolved_status=$?
python3 scripts/security/check-resolved-gradle.py "$java_scan_temp/resolved.json" "$resolved_status"
