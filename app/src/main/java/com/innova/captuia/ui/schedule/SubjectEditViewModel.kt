package com.innova.captuia.ui.schedule

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.innova.captuia.CaptuIAApplication
import com.innova.captuia.domain.repository.ScheduleRepository
import com.innova.captuia.domain.schedule.SessionInput
import com.innova.captuia.domain.schedule.SubjectFormResult
import com.innova.captuia.domain.schedule.SubjectFormValidator
import com.innova.captuia.domain.schedule.TimeOfDay
import com.innova.captuia.domain.usecase.ClassifyCapturesUseCase
import kotlinx.coroutines.launch
import java.time.DayOfWeek

/** Una franja en el formulario. [key] la identifica aunque cambie de posición. */
data class SessionRow(
    val key: Long,
    val day: DayOfWeek? = null,
    val start: String = "",
    val end: String = "",
)

class SubjectEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val schedule: ScheduleRepository,
    private val classifyCaptures: ClassifyCapturesUseCase,
) : ViewModel() {

    /** Null cuando se está creando una materia nueva. */
    val subjectId: Long? = savedStateHandle.get<String>(ARG_SUBJECT_ID)?.toLongOrNull()
    val isNew: Boolean get() = subjectId == null

    var name by mutableStateOf("")
        private set

    val rows = mutableStateListOf<SessionRow>()

    /** Problema del formulario que impide guardar; null si no hay ninguno que mostrar. */
    var error by mutableStateOf<SubjectFormResult?>(null)
        private set

    var isSaving by mutableStateOf(false)
        private set

    private var originalName: String? = null
    private var nextKey = 0L

    init {
        viewModelScope.launch {
            val existing = subjectId?.let { id -> schedule.subjects().firstOrNull { it.folder.id == id } }
            if (existing != null) {
                originalName = existing.name
                name = existing.name
                existing.sessions.forEach { session ->
                    rows += SessionRow(
                        key = nextKey++,
                        day = session.dayOfWeek,
                        start = TimeOfDay.format(session.startMinutes),
                        end = TimeOfDay.format(session.endMinutes),
                    )
                }
            } else {
                // Una materia nueva empieza con una franja vacía lista para llenar.
                rows += SessionRow(key = nextKey++)
            }
        }
    }

    fun onNameChange(value: String) {
        name = value
        error = null
    }

    fun addRow() {
        rows += SessionRow(key = nextKey++)
        error = null
    }

    fun removeRow(index: Int) {
        if (index in rows.indices) rows.removeAt(index)
        error = null
    }

    fun updateRow(index: Int, transform: (SessionRow) -> SessionRow) {
        if (index in rows.indices) rows[index] = transform(rows[index])
        error = null
    }

    /** Valida y guarda. [onSaved] se llama solo si la materia quedó guardada. */
    fun save(onSaved: () -> Unit) {
        if (isSaving) return
        isSaving = true
        viewModelScope.launch {
            try {
                val result = SubjectFormValidator.validate(
                    name = name,
                    sessions = rows.map { SessionInput(it.day, it.start, it.end) },
                    existingFolderNames = schedule.allFolderNames(),
                    otherSubjects = schedule.subjects().filter { it.folder.id != subjectId },
                    currentName = originalName,
                )
                if (result is SubjectFormResult.Valid) {
                    schedule.saveSubject(subjectId, result.name, result.sessions)
                    // El horario cambió: las capturas clasificadas por CaptuIA se evalúan de nuevo.
                    classifyCaptures.reclassifyAutomatic()
                    onSaved()
                } else {
                    error = result
                }
            } finally {
                isSaving = false
            }
        }
    }

    /** Elimina la materia. Sus capturas vuelven a clasificarse automáticamente. */
    fun delete(onDeleted: () -> Unit) {
        val id = subjectId ?: return
        viewModelScope.launch {
            schedule.deleteSubject(id)
            classifyCaptures()
            classifyCaptures.reclassifyAutomatic()
            onDeleted()
        }
    }

    companion object {
        const val ARG_SUBJECT_ID = "subjectId"

        /** Valor del argumento de la ruta para crear una materia. */
        const val NEW_SUBJECT = "nueva"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (
                    checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                        as CaptuIAApplication
                    ).container
                SubjectEditViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    schedule = container.scheduleRepository,
                    classifyCaptures = container.classifyCaptures,
                )
            }
        }
    }
}
