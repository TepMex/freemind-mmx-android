package org.freemind.mmx.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapApp(
    viewModel: MindMapViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val openDoc = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                ?: error("Unable to read document")
        }.onSuccess { xml ->
            val title = uri.lastPathSegment?.substringAfterLast('/') ?: "Opened map"
            viewModel.openXml(xml, title)
        }.onFailure { error ->
            viewModel.reportError("Failed to read document: ${error.message}")
        }
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
                val width = (120f + label.length * 7f).coerceIn(100f, 280f)
                LayoutNode(
                    id = node.id,
                    parentId = parentId,
                    text = label,
                    folded = node.folded,
                    side = node.side?.name?.lowercase(),
                    measuredWidth = width,
                    measuredHeight = 40f,
                )
            },
        )
    }
    val layout = remember(layoutInput) { layoutEngine.layout(layoutInput) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = state.documentTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    IconButton(onClick = { openDoc.launch(arrayOf("*/*", "text/xml", "application/xml")) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = stringResource(R.string.open_map))
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
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
            FloatingActionButton(onClick = viewModel::newMap) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_map))
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
            MindMapCanvas(
                layout = layout,
                selectedNodeId = state.selectedNodeId,
                onSelectNode = viewModel::selectNode,
                onToggleFold = viewModel::toggleFold,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}
