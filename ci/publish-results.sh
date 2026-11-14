#!/usr/bin/env bash
# Publica el resultado de la ejecución (registros, capturas y los APK de entrega) en la rama ci-results.
set -e
mkdir -p qa
# APK de entrega: el de arm64 para teléfonos y el universal para cualquier dispositivo o emulador.
cp app/build/outputs/apk/release/app-arm64-v8a-release.apk qa/captuia.apk 2>/dev/null || true
cp app/build/outputs/apk/release/app-universal-release.apk qa/captuia-universal.apk 2>/dev/null || true
ls -l app/build/outputs/apk/release/ > qa/apk-entrega.txt 2>/dev/null || true
# Permisos que declara el APK de entrega, para comprobar que no incluye el de internet.
AAPT=$(ls "${ANDROID_HOME:-/usr/local/lib/android/sdk}"/build-tools/*/aapt2 2>/dev/null | sort | tail -1)
if [ -n "$AAPT" ]; then
  "$AAPT" dump permissions qa/captuia.apk > qa/permisos-entrega.txt 2>&1 || true
fi
mkdir -p qa/resultados-unitarias
cp app/build/test-results/testDebugUnitTest/*.xml qa/resultados-unitarias/ 2>/dev/null || true
{
  echo "commit=${GITHUB_SHA}"
  echo "run=${GITHUB_RUN_ID}"
  echo "estado=${JOB_STATUS}"
  echo "fecha=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
} > qa/resumen.txt

cd qa
git init -q -b ci-results
git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git add -A
git commit -q -m "Resultados de CI para ${GITHUB_SHA}"
git push -q -f "https://x-access-token:${GITHUB_TOKEN}@github.com/${GITHUB_REPOSITORY}.git" ci-results
