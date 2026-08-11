package org.freemind.mmx.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.freemind.mmx.android.R
import org.freemind.mmx.android.ui.canvas.MindMapCanvas
import org.freemind.mmx.core.MindMapTree
import org.freemind.mmx.layout.FreeMindLayoutEngine
import org.freemind.mmx.layout.LayoutInput
import org.freemind.mmx.layout.LayoutNode
import org.freemind.mmx.layout.NodeTextMeasure

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapApp(
    viewModel: MindMapViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }

    val openDoc = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.openUri(uri)
    }
    val createMm = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-freemind"),
    ) { uri ->
        if (uri != null) viewModel.onMmCreated(uri)
    }
    val createMmx = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/xml"),
    ) { uri ->
        if (uri != null) viewModel.onMmxCreated(uri)
    }

    val layoutEngine = remember { FreeMindLayoutEngine() }
    val layoutInput = remember(state.map) {
        val flat = MindMapTree.flatten(state.map)
        LayoutInput(
            rootId = state.map.root.id,
            nodes = flat.map { (node, parentId) ->
                val label = node.text.ifBlank {
                    node.richContentHtml?.replace(Regex("<[^>]+>"), " ")?.trim().orEmpty()
                }.ifBlank { "(rich)" }
                val measured = NodeTextMeasure.measure(label)
                LayoutNode(
                    id = node.id,
                    parentId = parentId,
                    text = label,
                    folded = node.folded,
                    side = node.side?.name?.lowercase(),
                    measuredWidth = measured.nodeWidth,
                    measuredHeight = measured.nodeHeight,
                )
            },
        )
    }
    val layout = remember(layoutInput) { layoutEngine.layout(layoutInput) }
    val selected = state.selectedNodeId?.let { MindMapTree.find(state.map, it) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = buildString {
                            append(state.documentTitle)
                            if (state.isDirty) append(" •")
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    IconButton(
                        onClick = viewModel::undo,
                        enabled = state.canUndo,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = viewModel::redo,
                        enabled = state.canRedo,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    IconButton(onClick = { openDoc.launch(arrayOf("*/*", "text/xml", "application/xml")) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = stringResource(R.string.open_map))
                    }
                    IconButton(
                        onClick = {
                            viewModel.save {
                                createMm.launch(state.documentTitle.ifBlank { "mindmap.mm" })
                            }
                        },
                    ) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save_map))
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("New map") },
                            onClick = {
                                menuExpanded = false
                                viewModel.newMap()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Save As…") },
                            onClick = {
                                menuExpanded = false
                                createMm.launch(state.documentTitle.ifBlank { "mindmap.mm" })
                            },
                        )
                        if (state.pendingMmxSave || state.mmxUri == null) {
                            DropdownMenuItem(
                                text = { Text("Save MMX…") },
                                onClick = {
                                    menuExpanded = false
                                    val base = state.documentTitle.removeSuffix(".mm").removeSuffix(".MM")
                                    createMmx.launch(".$base.mmx")
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Sample map") },
                            onClick = {
                                menuExpanded = false
                                viewModel.loadAsset("maps/sample.mm")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Unicode sample") },
                            onClick = {
                                menuExpanded = false
                                viewModel.loadAsset("maps/unicode.mm")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Legacy test map") },
                            onClick = {
                                menuExpanded = false
                                viewModel.loadAsset("maps/legacy-testmap.mm")
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::addChild) {
                Icon(
                    Icons.Filled.SubdirectoryArrowRight,
                    contentDescription = stringResource(R.string.add_child),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Text(
                text = stringResource(R.string.milestone_banner),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
            state.statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            EditorActionBar(
                hasSelection = selected != null,
                onEdit = viewModel::requestEditSelected,
                onAddChild = viewModel::addChild,
                onAddSibling = viewModel::addSibling,
                onDelete = viewModel::requestDeleteSelected,
                onMoveUp = { viewModel.moveSelected(-1) },
                onMoveDown = { viewModel.moveSelected(1) },
                onToggleSide = viewModel::toggleSelectedSide,
            )
            MindMapCanvas(
                layout = layout,
                selectedNodeId = state.selectedNodeId,
                onSelectNode = viewModel::selectNode,
                onToggleFold = viewModel::toggleFold,
                onEditNode = viewModel::requestEdit,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }

    val editingId = state.editingNodeId
    if (editingId != null) {
        val node = MindMapTree.find(state.map, editingId)
        if (node != null) {
            EditNodeDialog(
                initialText = node.text.ifBlank {
                    node.richContentHtml?.replace(Regex("<[^>]+>"), " ")?.trim().orEmpty()
                },
                onDismiss = viewModel::dismissEdit,
                onConfirm = { viewModel.commitNodeText(editingId, it) },
            )
        } else {
            LaunchedEffect(editingId) { viewModel.dismissEdit() }
        }
    }

    val deleteId = state.confirmDeleteNodeId
    if (deleteId != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text(stringResource(R.string.delete_node_title)) },
            text = { Text(stringResource(R.string.delete_node_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun EditorActionBar(
    hasSelection: Boolean,
    onEdit: () -> Unit,
    onAddChild: () -> Unit,
    onAddSibling: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleSide: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledTonalIconButton(onClick = onEdit, enabled = hasSelection) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_node))
        }
        FilledTonalIconButton(onClick = onAddChild) {
            Icon(
                Icons.Filled.SubdirectoryArrowRight,
                contentDescription = stringResource(R.string.add_child),
            )
        }
        FilledTonalIconButton(onClick = onAddSibling, enabled = hasSelection) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_sibling))
        }
        FilledTonalIconButton(onClick = onDelete, enabled = hasSelection) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
        }
        FilledTonalIconButton(onClick = onMoveUp, enabled = hasSelection) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
        }
        FilledTonalIconButton(onClick = onMoveDown, enabled = hasSelection) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
        }
        TextButton(onClick = onToggleSide, enabled = hasSelection) {
            Text(stringResource(R.string.toggle_side))
        }
    }
}

@Composable
private fun EditNodeDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_node)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 12,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
