package org.freemind.mmx.android.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.freemind.mmx.android.data.MindMapDocumentRepository
import org.freemind.mmx.core.AddChildCommand
import org.freemind.mmx.core.AddSiblingCommand
import org.freemind.mmx.core.MindMap
import org.freemind.mmx.core.MindMapNode
import org.freemind.mmx.core.MindMapTree
import org.freemind.mmx.core.MoveAmongSiblingsCommand
import org.freemind.mmx.core.NodeSide
import org.freemind.mmx.core.RemoveSubtreeCommand
import org.freemind.mmx.core.SetNodeSideCommand
import org.freemind.mmx.core.SetNodeTextCommand
import org.freemind.mmx.core.ToggleFoldedCommand
import org.freemind.mmx.core.UndoRedoStack
import org.freemind.mmx.format.FreeMindFormat
import org.freemind.mmx.format.WriteOptions

data class EditorUiState(
    val documentTitle: String = "New mind map",
    val map: MindMap = MindMap.blank("New mind map"),
    val selectedNodeId: String? = null,
    val statusMessage: String? = null,
    val isLoading: Boolean = false,
    val isDirty: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val mmUri: Uri? = null,
    val mmxUri: Uri? = null,
    val pendingMmxSave: Boolean = false,
    val editingNodeId: String? = null,
    val confirmDeleteNodeId: String? = null,
)

class MindMapViewModel(application: Application) : AndroidViewModel(application) {
    private val format = FreeMindFormat()
    private val documents = MindMapDocumentRepository(application, format)
    private val undoStack = UndoRedoStack()

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    init {
        loadAsset("maps/sample.mm", title = "sample.mm")
    }

    fun newMap() {
        undoStack.clear()
        val map = MindMap.blank("New mind map")
        _state.value = EditorUiState(
            documentTitle = "Untitled.mm",
            map = map,
            selectedNodeId = map.root.id,
            statusMessage = "Created blank map",
            isDirty = true,
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
                undoStack.clear()
                _state.value = EditorUiState(
                    documentTitle = title,
                    map = map,
                    selectedNodeId = map.root.id,
                    statusMessage = "Opened $title",
                    isLoading = false,
                )
            }.onFailure { error ->
                _state.update {
                    it.copy(isLoading = false, statusMessage = "Failed to open $title: ${error.message}")
                }
            }
        }
    }

    fun openUri(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, statusMessage = "Opening…") }
            runCatching { documents.open(uri) }
                .onSuccess { opened ->
                    undoStack.clear()
                    _state.value = EditorUiState(
                        documentTitle = opened.title,
                        map = opened.map,
                        selectedNodeId = opened.map.root.id,
                        statusMessage = listOfNotNull("Opened ${opened.title}", opened.mmxWarning)
                            .joinToString(" — "),
                        isLoading = false,
                        mmUri = opened.mmUri,
                        mmxUri = opened.mmxUri,
                    )
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isLoading = false, statusMessage = "Open failed: ${error.message}")
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
                undoStack.clear()
                _state.value = EditorUiState(
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
        _state.update { it.copy(selectedNodeId = id, confirmDeleteNodeId = null) }
    }

    fun requestEditSelected() {
        val id = _state.value.selectedNodeId ?: return
        _state.update { it.copy(editingNodeId = id) }
    }

    fun requestEdit(id: String) {
        _state.update { it.copy(selectedNodeId = id, editingNodeId = id) }
    }

    fun dismissEdit() {
        _state.update { it.copy(editingNodeId = null) }
    }

    fun commitNodeText(nodeId: String, newText: String) {
        val node = MindMapTree.find(_state.value.map, nodeId) ?: return
        if (node.text == newText && node.richContentHtml == null) {
            dismissEdit()
            return
        }
        applyCommand(
            SetNodeTextCommand(
                nodeId = nodeId,
                oldText = node.text,
                newText = newText,
                oldRichContentHtml = node.richContentHtml,
                oldModifiedAtMillis = node.modifiedAtMillis,
            ),
            selectedId = nodeId,
            status = "Edited node",
        )
        dismissEdit()
    }

    fun addChild() {
        val parentId = _state.value.selectedNodeId ?: return
        val child = MindMapNode(
            id = MindMapNode.newId(),
            text = "New node",
            createdAtMillis = MindMapTree.nowMillis(),
            modifiedAtMillis = MindMapTree.nowMillis(),
        )
        applyCommand(AddChildCommand(parentId, child), selectedId = child.id, status = "Added child")
    }

    fun addSibling() {
        val selected = _state.value.selectedNodeId ?: return
        if (selected == _state.value.map.root.id) {
            addChild()
            return
        }
        val child = MindMapNode(
            id = MindMapNode.newId(),
            text = "New node",
            createdAtMillis = MindMapTree.nowMillis(),
            modifiedAtMillis = MindMapTree.nowMillis(),
        )
        applyCommand(AddSiblingCommand(selected, child), selectedId = child.id, status = "Added sibling")
    }

    fun requestDeleteSelected() {
        val id = _state.value.selectedNodeId ?: return
        if (id == _state.value.map.root.id) {
            _state.update { it.copy(statusMessage = "Cannot delete the root node") }
            return
        }
        _state.update { it.copy(confirmDeleteNodeId = id) }
    }

    fun confirmDelete() {
        val id = _state.value.confirmDeleteNodeId ?: return
        val parentId = MindMapTree.findParent(_state.value.map, id)?.parent?.id
        applyCommand(RemoveSubtreeCommand(id), selectedId = parentId, status = "Deleted subtree")
        _state.update { it.copy(confirmDeleteNodeId = null) }
    }

    fun dismissDelete() {
        _state.update { it.copy(confirmDeleteNodeId = null) }
    }

    fun moveSelected(delta: Int) {
        val id = _state.value.selectedNodeId ?: return
        applyCommand(MoveAmongSiblingsCommand(id, delta), selectedId = id, status = "Moved node")
    }

    fun toggleSelectedSide() {
        val id = _state.value.selectedNodeId ?: return
        if (MindMapTree.findParent(_state.value.map, id)?.parent?.id != _state.value.map.root.id) {
            _state.update { it.copy(statusMessage = "Side applies to first-level nodes only") }
            return
        }
        val node = MindMapTree.find(_state.value.map, id) ?: return
        val newSide = when (node.side) {
            NodeSide.LEFT -> NodeSide.RIGHT
            NodeSide.RIGHT -> NodeSide.LEFT
            null -> NodeSide.RIGHT
        }
        applyCommand(SetNodeSideCommand(id, node.side, newSide), selectedId = id, status = "Changed side")
    }

    fun toggleFold(id: String) {
        applyCommand(ToggleFoldedCommand(id), selectedId = id, status = null, markDirty = true)
    }

    fun undo() {
        val current = _state.value
        if (!undoStack.canUndo) return
        val map = undoStack.undo(current.map)
        _state.value = current.copy(
            map = map,
            isDirty = true,
            canUndo = undoStack.canUndo,
            canRedo = undoStack.canRedo,
            statusMessage = "Undo",
        )
    }

    fun redo() {
        val current = _state.value
        if (!undoStack.canRedo) return
        val map = undoStack.redo(current.map)
        _state.value = current.copy(
            map = map,
            isDirty = true,
            canUndo = undoStack.canUndo,
            canRedo = undoStack.canRedo,
            statusMessage = "Redo",
        )
    }

    fun save(onNeedCreateDocument: () -> Unit) {
        val current = _state.value
        val uri = current.mmUri
        if (uri == null) {
            onNeedCreateDocument()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, statusMessage = "Saving…") }
            runCatching {
                documents.save(current.map, uri, current.mmxUri)
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isDirty = false,
                        mmUri = result.mmUri,
                        mmxUri = result.mmxUri,
                        documentTitle = result.title,
                        pendingMmxSave = result.mmxUri == null && result.warning != null,
                        statusMessage = result.warning ?: "Saved ${result.title}",
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(isLoading = false, statusMessage = "Save failed: ${error.message}")
                }
            }
        }
    }

    fun onMmCreated(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, mmUri = uri, statusMessage = "Saving…") }
            runCatching {
                documents.save(_state.value.map, uri, _state.value.mmxUri)
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isDirty = false,
                        mmUri = result.mmUri,
                        mmxUri = result.mmxUri,
                        documentTitle = result.title,
                        pendingMmxSave = result.mmxUri == null && result.warning != null,
                        statusMessage = result.warning ?: "Saved ${result.title}",
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(isLoading = false, statusMessage = "Save As failed: ${error.message}")
                }
            }
        }
    }

    fun onMmxCreated(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { documents.writeMmxOnly(uri, _state.value.map) }
            }.onSuccess {
                _state.update {
                    it.copy(
                        mmxUri = uri,
                        pendingMmxSave = false,
                        statusMessage = "Saved MMX sidecar",
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(statusMessage = "MMX save failed: ${error.message}") }
            }
        }
    }

    fun exportMmXml(): String = format.writeMm(_state.value.map, WriteOptions())
    fun exportMmxXml(): String = format.writeMmx(_state.value.map, WriteOptions())

    fun reportError(message: String) {
        _state.update { it.copy(isLoading = false, statusMessage = message) }
    }

    private fun applyCommand(
        command: org.freemind.mmx.core.MindMapCommand,
        selectedId: String?,
        status: String?,
        markDirty: Boolean = true,
    ) {
        val current = _state.value
        val next = undoStack.execute(current.map, command)
        _state.value = current.copy(
            map = next,
            selectedNodeId = selectedId ?: current.selectedNodeId,
            isDirty = if (markDirty) true else current.isDirty,
            canUndo = undoStack.canUndo,
            canRedo = undoStack.canRedo,
            statusMessage = status ?: current.statusMessage,
        )
    }
}
