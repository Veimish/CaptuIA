package com.innova.captuia.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.innova.captuia.CaptuIAApplication
import com.innova.captuia.domain.model.Subject
import com.innova.captuia.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ScheduleUiState(
    val isLoading: Boolean = true,
    val subjects: List<Subject> = emptyList(),
)

class ScheduleViewModel(schedule: ScheduleRepository) : ViewModel() {

    val state: StateFlow<ScheduleUiState> =
        schedule.observeSubjects()
            .map { ScheduleUiState(isLoading = false, subjects = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleUiState())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (
                    checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                        as CaptuIAApplication
                    ).container
                ScheduleViewModel(container.scheduleRepository)
            }
        }
    }
}
