package com.waktusolat.app.ui.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waktusolat.app.data.repository.QuranRepository
import com.waktusolat.app.domain.model.Surah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QuranUiState(
    val surahs: List<Surah> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = ""
)

class QuranViewModel(
    private val repository: QuranRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuranUiState())
    val uiState: StateFlow<QuranUiState> = _uiState.asStateFlow()

    init { loadSurahs() }

    fun loadSurahs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.refreshSurahs()
            result.fold(
                onSuccess = {
                    repository.getAllSurahs().collect { surahs ->
                        _uiState.value = _uiState.value.copy(surahs = surahs, isLoading = false)
                    }
                },
                onFailure = { e ->
                    repository.getAllSurahs().collect { surahs ->
                        _uiState.value = _uiState.value.copy(
                            surahs = surahs, isLoading = false,
                            error = if (surahs.isEmpty()) e.message else null
                        )
                    }
                }
            )
        }
    }

    fun searchSurahs(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        viewModelScope.launch {
            if (query.isBlank()) {
                repository.getAllSurahs().collect { surahs ->
                    _uiState.value = _uiState.value.copy(surahs = surahs)
                }
            } else {
                repository.searchSurahs(query).collect { surahs ->
                    _uiState.value = _uiState.value.copy(surahs = surahs)
                }
            }
        }
    }

    fun toggleBookmark(surahId: Int, currentState: Boolean) {
        viewModelScope.launch { repository.toggleBookmark(surahId, currentState) }
    }
}
