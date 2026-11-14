# Reglas de R8 propias de CaptuIA.
#
# Room, WorkManager, ML Kit, Coil y Compose traen sus propias reglas, y el código
# de la app no usa reflexión, así que no hace falta conservar clases a mano.

# Conserva archivo y número de línea para poder leer un informe de error.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
