# CaptuIA

Aplicación Android que organiza las capturas de pantalla por su contenido.
Proyecto del Equipo INNOVA — Universidad Libre, Facultad de Ingeniería (Ingeniería Aplicada 2026-2).

## Estado

- **Etapa 0 — Base del proyecto:** identidad visual, permiso de fotos y detección de las capturas del dispositivo.
- **Etapa 1 — Galería:** índice local, capturas agrupadas por día, detalle con zoom y compartir.
- **Etapa 2 — Texto y búsqueda:** lectura de texto en el dispositivo (ML Kit) en segundo plano y buscador por contenido que no distingue tildes ni mayúsculas.
- **Etapa 3 — Carpetas:** clasificación automática por reglas (Documentos, Tareas, Cursos, Imágenes), mover capturas a mano y carpetas propias.
- **Etapa 4 — Horario de clases:** formulario para registrar materias con sus días y horas; cada materia tiene su carpeta y recibe las capturas que la mencionan o las de contenido académico tomadas durante su clase.

## Tecnología

Kotlin · Jetpack Compose · arquitectura MVVM en capas (`ui`, `domain`, `data`, `di`).
Todo el procesamiento ocurre en el dispositivo: las capturas no se envían a ningún servidor.

## Compilar

Abrir la carpeta en Android Studio y ejecutar, o desde la terminal:

```
./gradlew assembleDebug
```

## Pruebas automáticas

Cada cambio en `main` se compila y se prueba en un emulador de Android 14 con GitHub Actions
(`.github/workflows/ci.yml`). Los resultados quedan en la rama `ci-results`: registros,
una imagen de cada pantalla probada y el APK de depuración.
