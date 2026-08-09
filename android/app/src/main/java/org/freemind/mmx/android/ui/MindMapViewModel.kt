package org.freemind.mmx.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.freemind.mmx.core.MindMap
import org.freemind.mmx.core.MindMapTree
import org.freemind.mmx.format.FreeMindFormat

data class ViewerUiState(
    val documentTitle: String = "New mind map",
    val map: MindMap = MindMap.blank("New mind map"),
    val selectedNodeId: String? = null,
    val statusMessage: String? = null,
    val isLoading: Boolean = false,
)

class MindMapViewModel(application: Application) : AndroidViewModel(application) {
    private val format = FreeMindFormat()
    private val _state = MutableStateFlow(ViewerUiState())
    val state: StateFlow<ViewerUiState> = _state.asStateFlow()

    init {
        loadAsset("maps/sample.mm", title = "sample.mm")
    }

    fun newMap() {
        val map = MindMap.blank("New mind map")
        _state.value = ViewerUiState(
            documentTitle = "Untitled",
            map = map,
            selectedNodeId = map.root.id,
            statusMessage = "Created blank map",
        )
    }

    fun loadAsset(assetPath: String, title: String = assetPath.substringAfterLast('/')) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, statusMessage = "Opening $title…") }
            runCatching {
                withContext(Dispatchers.IO) {
                    val xml = getApplication<Application>().assets.open(assetPath)
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                    format.parseMm(xml)
                }
            }.onSuccess { map ->
                _state.value = ViewerUiState(
                    documentTitle = title,
                    map = map,
                    selectedNodeId = map.root.id,
                    statusMessage = "Opened $title",
                    isLoading = false,
                )
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = "Failed to open $title: ${error.message}",
                    )
                }
            }
        }
    }

    fun openXml(xml: String, title: String, mmxXml: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching {
                withContext(Dispatchers.Default) { format.parseMm(xml, mmxXml) }
            }.onSuccess { map ->
                _state.value = ViewerUiState(
                    documentTitle = title,
                    map = map,
                    selectedNodeId = map.root.id,
                    statusMessage = "Opened $title",
                    isLoading = false,
                )
            }.onFailure { error ->
                _state.update {
                    it.copy(isLoading = false, statusMessage = "Open failed: ${error.message}")
                }
            }
        }
    }

    fun selectNode(id: String?) {
        _state.update { it.copy(selectedNodeId = id) }
    }

    fun toggleFold(id: String) {
        _state.update { current ->
            current.copy(map = MindMapTree.toggleFolded(current.map, id))
        }
    }

    fun reportError(message: String) {
        _state.update { it.copy(isLoading = false, statusMessage = message) }
    }
}
