#!/usr/bin/env python3
"""Comprueba en el emulador el APK de entrega (release, con el código reducido por R8).

El recorrido completo de la interfaz se ejecuta sobre la compilación de depuración.
Este guion instala el APK que realmente se entrega y verifica, manejándolo como un
usuario (toques y teclado), que arranca, lee y clasifica las capturas, guarda una
materia y abre el detalle de una captura. Usa las capturas que dejó el recorrido.
"""
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = "com.innova.captuia"
APK = "app/build/outputs/apk/release/app-x86_64-release.apk"
SCREENS = "qa/pantallas"


def adb(*args, check=False, binary=False):
    result = subprocess.run(["adb", *args], capture_output=True, check=check)
    return result.stdout if binary else result.stdout.decode("utf-8", "replace")


def screenshot(name):
    subprocess.run(["mkdir", "-p", SCREENS])
    with open(f"{SCREENS}/{name}.png", "wb") as image:
        image.write(adb("exec-out", "screencap", "-p", binary=True))


# Lo último que respondió el sistema al pedir la pantalla, para diagnosticar un fallo.
last_dump = {"message": "", "labels": []}


def dump():
    """Árbol de la pantalla actual, o None si el sistema no pudo entregarlo."""
    adb("shell", "rm", "-f", "/sdcard/captuia-ui.xml")
    result = subprocess.run(
        ["adb", "shell", "uiautomator", "dump", "/sdcard/captuia-ui.xml"],
        capture_output=True, text=True,
    )
    last_dump["message"] = f"{result.stdout.strip()} {result.stderr.strip()}".strip()
    try:
        root = ET.fromstring(adb("exec-out", "cat", "/sdcard/captuia-ui.xml", binary=True))
    except ET.ParseError:
        return None
    last_dump["labels"] = [label(node).strip() for node in root.iter("node") if label(node).strip()]
    return root


def label(node):
    return f"{node.get('text', '')} {node.get('content-desc', '')}"


def find(root, predicate):
    return [node for node in root.iter("node") if predicate(node)] if root is not None else []


def has(*parts):
    """Nodo cuyo texto contiene todas las partes dadas."""
    return lambda node: all(part in label(node) for part in parts)


def fail(message):
    print(f"ENTREGA: FALLO - {message}")
    print(f"ENTREGA: última respuesta al leer la pantalla: {last_dump['message']}")
    print(f"ENTREGA: textos en pantalla: {last_dump['labels']}")
    screenshot("99-fallo-entrega")
    print(adb("logcat", "-d", "-t", "300", "*:E"))
    sys.exit(1)


def wait_for(description, predicate, timeout=60):
    """Espera hasta que haya un nodo que cumpla la condición y devuelve el primero."""
    deadline = time.time() + timeout
    while time.time() < deadline:
        found = find(dump(), predicate)
        if found:
            print(f"ENTREGA: ok - {description}")
            return found[0]
        time.sleep(2)
    fail(f"no apareció: {description}")


def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(1)


def tap_when_visible(description, predicate, timeout=60):
    tap(wait_for(description, predicate, timeout))


def hide_keyboard():
    """Cierra el teclado en pantalla si está abierto, para que no tape el formulario."""
    if "mInputShown=true" in adb("shell", "dumpsys", "input_method"):
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(1)


def type_in_field(index, text):
    """Escribe en el campo de texto número [index] de la pantalla (0 es el primero)."""
    fields = lambda node: node.get("class") == "android.widget.EditText"
    wait_for(f"campo de texto {index}", fields)
    found = find(dump(), fields)
    if len(found) <= index:
        fail(f"no existe el campo de texto {index}")
    tap(found[index])
    adb("shell", "input", "text", text)
    time.sleep(1)
    hide_keyboard()


def main():
    adb("uninstall", PKG)
    adb("uninstall", f"{PKG}.test")
    adb("logcat", "-c")
    installed = subprocess.run(["adb", "install", APK], capture_output=True, text=True)
    print(installed.stdout, installed.stderr)
    if "Success" not in installed.stdout:
        fail("no se pudo instalar el APK de entrega")
    adb("shell", "pm", "grant", PKG, "android.permission.READ_MEDIA_IMAGES")
    adb("shell", "am", "start", "-W", "-n", f"{PKG}/.MainActivity")

    # 1. Arranca, encuentra las cinco capturas, lee su texto y las clasifica.
    wait_for("Inicio con las 5 capturas", has("Todas las capturas", "5 capturas"), timeout=180)
    wait_for("Tareas con 2 capturas", has("Tareas", "2 capturas"), timeout=300)
    wait_for("Cursos con 2 capturas", has("Cursos", "2 capturas"), timeout=120)
    wait_for("Imágenes con 1 captura", has("genes", "1 captura"), timeout=120)
    screenshot("39-entrega-inicio")

    # 2. Registra una materia con su clase desde el formulario.
    tap_when_visible("acceso al horario", has("Horario de clases", "Registra tus materias"))
    tap_when_visible("botón Agregar materia", has("Agregar materia"))
    wait_for("formulario de materia", has("Guardar materia"))
    type_in_field(0, "Algebra")
    tap_when_visible("día lunes", lambda node: node.get("content-desc") == "Lunes")
    type_in_field(1, "8:00")
    type_in_field(2, "10:00")
    tap_when_visible("botón Guardar materia", has("Guardar materia"))
    wait_for("materia guardada en el horario", has("Algebra", "08:00"))
    screenshot("40-entrega-horario")

    # 3. Vuelve a Inicio, abre una carpeta y el detalle de una captura.
    hide_keyboard()
    adb("shell", "input", "keyevent", "BACK")
    wait_for("Inicio con la materia registrada", has("Horario de clases", "1 materia registrada"))
    tap_when_visible("carpeta Imágenes", has("genes", "1 captura"))
    tap_when_visible("miniatura de la captura", has("Captura del"))
    wait_for("detalle de la captura", has("Compartir"))
    screenshot("41-entrega-detalle")

    # 4. La app sigue viva y no registró ningún cierre inesperado.
    if not adb("shell", "pidof", PKG).strip():
        fail("la app ya no está en ejecución")
    if f"Process: {PKG}" in adb("logcat", "-d", "AndroidRuntime:E", "*:S"):
        fail("el registro del sistema contiene un cierre inesperado")
    print("ENTREGA: APROBADO")


if __name__ == "__main__":
    main()
