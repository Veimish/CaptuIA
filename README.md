# CaptuIA

Aplicación Android que organiza las capturas de pantalla por su contenido.
Proyecto del Equipo INNOVA — Universidad Libre, Facultad de Ingeniería (Ingeniería Aplicada 2026-2).

Todo el procesamiento ocurre en el teléfono: las capturas no se envían a ningún servidor
y la app no pide permiso de internet.

## Qué hace

- **Encuentra tus capturas.** Detecta las capturas de pantalla de la galería y las muestra agrupadas por día.
- **Lee su contenido.** Reconoce el texto de cada captura en segundo plano (ML Kit, en el dispositivo).
- **Busca por contenido.** El buscador encuentra palabras que aparecen dentro de las capturas, sin distinguir tildes ni mayúsculas.
- **Clasifica sola.** Reparte las capturas en Documentos, Tareas, Cursos e Imágenes según su texto, y explica por qué eligió cada carpeta.
- **Usa tu horario de clases.** Registras tus materias con día y hora; cada materia tiene su carpeta y recibe las capturas que la mencionan o las de contenido académico tomadas durante su clase.
- **Te deja corregir.** Puedes mover una captura a otra carpeta y crear carpetas propias. Lo que mueves a mano no se vuelve a mover.

## Instalar

Requiere Android 8.0 o superior.

1. Descarga el APK desde la rama [`ci-results`](../../tree/ci-results):
   - [`captuia.apk`](../../raw/ci-results/captuia.apk) — para teléfonos (arm64).
   - [`captuia-universal.apk`](../../raw/ci-results/captuia-universal.apk) — para cualquier dispositivo o emulador; pesa más.
2. Ábrelo en el teléfono y acepta instalar aplicaciones de esa fuente.
3. Al abrir CaptuIA, permite el acceso a tus fotos para que encuentre las capturas.

El APK está firmado con una clave de depuración que cambia en cada compilación: para
instalar una versión nueva hay que desinstalar la anterior.

## Tecnología

Kotlin · Jetpack Compose · Room · WorkManager · ML Kit (reconocimiento de texto) · Coil.

Arquitectura MVVM en capas dentro de `app/src/main/java/com/innova/captuia`:

| Capa | Contenido |
|---|---|
| `ui` | Pantallas de Compose, sus ViewModel, componentes y tema visual |
| `domain` | Modelos, reglas de clasificación y búsqueda, validaciones, casos de uso e interfaces de repositorio |
| `data` | Acceso a la galería (MediaStore), base de datos local (Room), lector de texto y trabajo en segundo plano |
| `di` | `AppContainer`, que crea y conecta las dependencias |

La clasificación es por reglas (`domain/classify/CaptureClassifier.kt`): palabras clave con peso,
nombres de materias y franjas del horario. No usa servicios de pago ni conexión a internet.

## Compilar

Abrir la carpeta en Android Studio y ejecutar, o desde la terminal (JDK 17):

```
./gradlew assembleDebug      # APK de depuración
./gradlew assembleRelease    # APK de entrega, con el código reducido por R8
./gradlew testDebugUnitTest  # pruebas unitarias
```

## Pruebas automáticas

Cada cambio en `main` se compila y se prueba con GitHub Actions (`.github/workflows/ci.yml`):

- **Pruebas unitarias** de las reglas: clasificación, búsqueda, validación de nombres y del horario.
- **Recorrido de la interfaz** en un emulador de Android 14: un usuario simulado concede el permiso,
  toma capturas, busca, mueve capturas, crea carpetas y registra su horario
  (`app/src/androidTest`). Se guarda una imagen de cada paso.
- **Comprobación del APK de entrega** (`ci/smoke_release.py`): se instala el APK final en el emulador
  y se verifica que arranca, lee y clasifica las capturas, guarda una materia y abre un detalle.

Los resultados quedan en la rama `ci-results`: registros, imágenes de cada pantalla y los APK.

## Limitaciones conocidas

- Las capturas nuevas se detectan al abrir o volver a la app, no en segundo plano.
- La clasificación depende del texto: una captura sin palabras reconocibles va a Documentos o a Imágenes.
- El reconocimiento de texto es para alfabeto latino y no está pensado para texto manuscrito.
- Las pruebas automáticas corren en un emulador de Android 14; no se ha verificado en otras versiones.
