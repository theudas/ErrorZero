package com.errorzero.app.ui.manage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.errorzero.app.app
import com.errorzero.app.data.Subject
import com.errorzero.app.data.TagType
import com.errorzero.app.data.TagUsage
import com.errorzero.app.ui.components.ConfirmDialog
import com.errorzero.app.ui.components.EmptyState
import com.errorzero.app.ui.components.Hairline
import com.errorzero.app.ui.components.TextInputDialog
import com.errorzero.app.ui.components.TextTab
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageScaffold(
    title: String,
    onBack: () -> Unit,
    snackbar: SnackbarHostState,
    onAdd: (() -> Unit)? = null,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回") } },
                actions = { if (onAdd != null) IconButton(onClick = onAdd) { Icon(Icons.Outlined.Add, "新增") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        content = content,
    )
}

@Composable
fun SubjectsScreen(onBack: () -> Unit) {
    val repo = LocalContext.current.app.repository
    val subjects by repo.subjects.collectAsStateWithLifecycle(initialValue = emptyList())
    val counts by repo.subjectCounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val countMap = counts.associate { it.subjectId to it.count }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Subject?>(null) }
    var deleting by remember { mutableStateOf<Subject?>(null) }

    ManageScaffold("科目管理", onBack, snackbar, onAdd = { adding = true }) { padding ->
        LazyColumn(contentPadding = padding) {
            itemsIndexed(subjects, key = { _, s -> s.id }) { index, s ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { renaming = s }
                        .padding(start = 24.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(s.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(
                        "${countMap[s.id] ?: 0} 题",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        scope.launch { repo.reorderSubjects(subjects.toMutableList().apply { add(index - 1, removeAt(index)) }) }
                    }, enabled = index > 0) { Icon(Icons.Outlined.KeyboardArrowUp, "上移") }
                    IconButton(onClick = {
                        scope.launch { repo.reorderSubjects(subjects.toMutableList().apply { add(index + 1, removeAt(index)) }) }
                    }, enabled = index < subjects.lastIndex) { Icon(Icons.Outlined.KeyboardArrowDown, "下移") }
                    IconButton(onClick = { deleting = s }) {
                        Icon(Icons.Outlined.DeleteOutline, "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Hairline(Modifier.padding(start = 24.dp))
            }
            item {
                Text(
                    "点击科目名称可重命名",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }

    if (adding) {
        TextInputDialog("新科目", placeholder = "科目名称", onDismiss = { adding = false }) { name ->
            scope.launch { repo.addSubject(name) }
        }
    }
    renaming?.let { s ->
        TextInputDialog("重命名科目", initial = s.name, onDismiss = { renaming = null }) { name ->
            scope.launch { if (!repo.renameSubject(s, name)) snackbar.showSnackbar("已存在同名科目") }
        }
    }
    deleting?.let { s ->
        ConfirmDialog(
            title = "删除「${s.name}」？",
            text = "仅能删除没有错题的科目。",
            confirmText = "删除",
            destructive = true,
            onDismiss = { deleting = null },
            onConfirm = {
                scope.launch {
                    val n = repo.deleteSubject(s.id)
                    if (n > 0) snackbar.showSnackbar("该科目下还有 $n 道错题，请先移动或删除")
                }
            },
        )
    }
}

@Composable
fun TagsScreen(onBack: () -> Unit) {
    val repo = LocalContext.current.app.repository
    val tags by repo.tagUsage.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var type by rememberSaveable { mutableIntStateOf(TagType.REASON) }
    var renaming by remember { mutableStateOf<TagUsage?>(null) }
    var deleting by remember { mutableStateOf<TagUsage?>(null) }

    val shown = tags.filter { it.type == type }

    ManageScaffold("标签管理", onBack, snackbar) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                    TextTab("错因", type == TagType.REASON, { type = TagType.REASON })
                    Spacer(Modifier.width(24.dp))
                    TextTab("模块", type == TagType.MODULE, { type = TagType.MODULE })
                }
                Hairline()
            }
            if (shown.isEmpty()) {
                item { EmptyState("暂无标签", "在记录错题时添加的标签会出现在这里") }
            }
            itemsIndexed(shown, key = { _, t -> t.id }) { _, t ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { renaming = t }
                        .padding(start = 24.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(t.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text("${t.count} 题", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = { deleting = t }) {
                        Icon(Icons.Outlined.DeleteOutline, "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Hairline(Modifier.padding(start = 24.dp))
            }
            if (shown.isNotEmpty()) {
                item {
                    Text(
                        "点击标签可重命名；改为已有名称时两个标签会合并",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }

    renaming?.let { t ->
        TextInputDialog("重命名标签", initial = t.name, onDismiss = { renaming = null }) { name ->
            scope.launch { repo.renameTag(t, name) }
        }
    }
    deleting?.let { t ->
        ConfirmDialog(
            title = "删除标签「${t.name}」？",
            text = if (t.count > 0) "将从 ${t.count} 道错题上移除此标签，错题本身不受影响。" else "该标签未被使用。",
            confirmText = "删除",
            destructive = true,
            onDismiss = { deleting = null },
            onConfirm = { scope.launch { repo.deleteTag(t.id) } },
        )
    }
}
