package org.freemind.mmx.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.freemind.mmx.android.R
import org.freemind.mmx.core.MindMap
import org.freemind.mmx.layout.LayoutInput
import org.freemind.mmx.layout.LayoutNode
import org.freemind.mmx.layout.StubMindMapLayoutEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapApp() {
    var map by remember { mutableStateOf(MindMap.blank("New mind map")) }
    val layoutEngine = StubMindMapLayoutEngine()
    val layout = layoutEngine.layout(
        LayoutInput(
            rootId = map.root.id,
            nodes = listOf(
                LayoutNode(
                    id = map.root.id,
                    parentId = null,
                    text = map.root.text,
                    folded = map.root.folded,
                    side = null,
                    measuredWidth = 160f,
                    measuredHeight = 48f,
                ),
            ),
        ),
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(map.root.text) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    IconButton(onClick = { /* Milestone 7: SAF open */ }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = stringResource(R.string.open_map))
                    }
                    IconButton(onClick = { /* Milestone 5: save */ }) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save_map))
                    }
                    IconButton(onClick = { /* overflow later */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { map = MindMap.blank("New mind map") },
            ) {
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textAlign = TextAlign.Center,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                val nodeColor = MaterialTheme.colorScheme.primary
                val onNode = MaterialTheme.colorScheme.onPrimary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    layout.nodes.forEach { node ->
                        val left = cx + node.bounds.left
                        val top = cy + node.bounds.top
                        drawRoundRect(
                            color = nodeColor,
                            topLeft = Offset(left, top),
                            size = Size(node.bounds.width, node.bounds.height),
                            cornerRadius = CornerRadius(12f, 12f),
                        )
                        drawRoundRect(
                            color = onNode.copy(alpha = 0.35f),
                            topLeft = Offset(left, top),
                            size = Size(node.bounds.width, node.bounds.height),
                            cornerRadius = CornerRadius(12f, 12f),
                            style = Stroke(width = 2f),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 64.dp)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = map.root.text,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    Text(
                        text = stringResource(R.string.status_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
