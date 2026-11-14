package com.innova.captuia.domain.folder

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderNameValidatorTest {

    private val existing = listOf("Documentos", "Tareas", "Cursos", "Imágenes", "Proyecto final")

    @Test
    fun `acepta un nombre nuevo y lo limpia`() {
        assertEquals(
            FolderNameCheck.Valid("Recetas de cocina"),
            FolderNameValidator.check("  Recetas   de cocina ", existing),
        )
    }

    @Test
    fun `rechaza vacio o solo espacios`() {
        assertEquals(FolderNameCheck.Empty, FolderNameValidator.check("", existing))
        assertEquals(FolderNameCheck.Empty, FolderNameValidator.check("    ", existing))
    }

    @Test
    fun `rechaza nombres de mas de 30 caracteres`() {
        assertEquals(FolderNameCheck.TooLong, FolderNameValidator.check("a".repeat(31), existing))
        assertEquals(FolderNameCheck.Valid("a".repeat(30)), FolderNameValidator.check("a".repeat(30), existing))
    }

    @Test
    fun `rechaza duplicados sin importar mayusculas ni tildes`() {
        assertEquals(FolderNameCheck.Duplicate, FolderNameValidator.check("proyecto FINAL", existing))
        assertEquals(FolderNameCheck.Duplicate, FolderNameValidator.check("imagenes", existing))
    }

    @Test
    fun `al renombrar permite conservar el mismo nombre o cambiarle mayusculas`() {
        assertEquals(
            FolderNameCheck.Valid("Proyecto Final"),
            FolderNameValidator.check("Proyecto Final", existing, currentName = "Proyecto final"),
        )
        assertEquals(
            FolderNameCheck.Duplicate,
            FolderNameValidator.check("Tareas", existing, currentName = "Proyecto final"),
        )
    }
}
