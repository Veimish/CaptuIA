#!/usr/bin/env bash
# Ejecuta las pruebas de interfaz en el emulador, recoge las capturas de cada paso
# y después comprueba el APK de entrega.
set -x
mkdir -p qa
adb shell rm -rf /sdcard/Download/captuia-qa /sdcard/Pictures/Screenshots
# El emulador de CI es lento al arrancar y a veces muestra "X isn't responding" sobre la app.
# Se ocultan esos diálogos del sistema para que no tapen la pantalla que se prueba.
adb shell settings put global hide_error_dialogs 1

# Capturas de ejemplo con contenido real (horario, tarea, chat, foto) para las pruebas.
adb shell rm -rf /data/local/tmp/captuia-muestras
adb push ci/muestras /data/local/tmp/captuia-muestras
adb logcat -c || true

./gradlew --no-daemon connectedDebugAndroidTest 2>&1 | tee qa/pruebas-dispositivo.log
status=${PIPESTATUS[0]}

adb pull /sdcard/Download/captuia-qa qa/pantallas || true
adb logcat -d -t 4000 > qa/logcat.txt || true
mkdir -p qa/resultados
find app/build/outputs/androidTest-results -name '*.xml' -exec cp {} qa/resultados/ \; || true

# El APK que se entrega (release) se instala y se comprueba aparte.
python3 ci/smoke_release.py 2>&1 | tee qa/entrega.log
entrega=${PIPESTATUS[0]}
adb logcat -d -t 4000 > qa/logcat-entrega.txt || true

if [ "$status" -ne 0 ]; then exit "$status"; fi
exit "$entrega"
