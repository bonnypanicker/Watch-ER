package com.watch_er.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watch_er.data.Watcher
import com.watch_er.data.WatcherRepository
import com.watch_er.data.WatcherStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WatchErUiState(
    val watchers: List<Watcher> = emptyList(),
    val selectedWatcher: Watcher? = null,
    val draftIntent: String = "",
    val draftMode: String = "oneshot",
    val draftThreshold: Int = 90,
    val draftStrategy: WatcherStrategy? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class WatchErViewModel(
    private val repository: WatcherRepository = WatcherRepository()
) : ViewModel() {
    private val mutableState = MutableStateFlow(WatchErUiState())
    val state: StateFlow<WatchErUiState> = mutableState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.loadWatchers() }
                .onSuccess { watchers ->
                    mutableState.update { it.copy(watchers = watchers, isLoading = false) }
                }
                .onFailure { throwable ->
                    mutableState.update { it.copy(isLoading = false, error = throwable.message) }
                }
        }
    }

    fun updateIntent(value: String) {
        mutableState.update { it.copy(draftIntent = value, draftStrategy = null) }
    }

    fun updateMode(value: String) {
        mutableState.update { it.copy(draftMode = value) }
    }

    fun updateThreshold(value: Int) {
        mutableState.update { it.copy(draftThreshold = value) }
    }

    fun generateStrategy() {
        val snapshot = mutableState.value
        if (snapshot.draftIntent.isBlank()) return
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.previewStrategy(snapshot.draftIntent) }
                .onSuccess { strategy ->
                    mutableState.update { it.copy(draftStrategy = strategy, isLoading = false) }
                }
                .onFailure { throwable ->
                    mutableState.update { it.copy(isLoading = false, error = throwable.message) }
                }
        }
    }

    fun createWatcher(onCreated: () -> Unit) {
        val snapshot = mutableState.value
        if (snapshot.draftIntent.isBlank()) return
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.createWatcher(
                    intent = snapshot.draftIntent,
                    mode = snapshot.draftMode,
                    threshold = snapshot.draftThreshold,
                    strategy = snapshot.draftStrategy
                )
            }.onSuccess { watcher ->
                mutableState.update {
                    it.copy(
                        watchers = it.watchers + watcher,
                        selectedWatcher = watcher,
                        draftIntent = "",
                        draftStrategy = null,
                        isLoading = false
                    )
                }
                onCreated()
            }.onFailure { throwable ->
                mutableState.update { it.copy(isLoading = false, error = throwable.message) }
            }
        }
    }

    fun selectWatcher(watcher: Watcher?) {
        mutableState.update { it.copy(selectedWatcher = watcher) }
    }
}
