package com.innova.captuia.domain.folder

import com.innova.captuia.domain.search.TextNormalizer

/** Resultado de revisar el nombre que el usuario escribió para una carpeta. */
sealed interface FolderNameCheck {
    /** [name] es el nombre ya limpio (sin espacios sobrantes). */
    data class Valid(val name: String) : FolderNameCheck
    data object Empty : FolderNameCheck
    data object TooLong : FolderNameCheck
    data object Duplicate : FolderNameCheck
}

object FolderNameValidator {

    /** Alcanza para nombres de materias largos, como "Programación Orientada a Objetos". */
    const val MAX_LENGTH = 40

    private val spaces = Regex("\\s+")

    /** Clave para comparar nombres sin distinguir mayúsculas, tildes ni espacios repetidos. */
    fun key(name: String): String = TextNormalizer.normalize(name)

    /**
     * Revisa [input] contra los nombres que ya existen ([existingNames]).
     * Al renombrar, [currentName] es el nombre actual de la carpeta, que no
     * cuenta como duplicado de sí misma.
     */
    fun check(
        input: String,
        existingNames: Collection<String>,
        currentName: String? = null,
    ): FolderNameCheck {
        val name = spaces.replace(input, " ").trim()
        if (name.isEmpty()) return FolderNameCheck.Empty
        if (name.length > MAX_LENGTH) return FolderNameCheck.TooLong

        val nameKey = key(name)
        val ownKey = currentName?.let { key(it) }
        val taken = existingNames.any { key(it) == nameKey } && nameKey != ownKey
        return if (taken) FolderNameCheck.Duplicate else FolderNameCheck.Valid(name)
    }
}
