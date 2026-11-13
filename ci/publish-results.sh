#!/usr/bin/env bash
# Publica el resultado de la ejecución (registros, capturas y APK) en la rama ci-results.
set -e
mkdir -p qa
cp app/build/outputs/apk/debug/app-debug.apk qa/captuia-debug.apk 2>/dev/null || true
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
