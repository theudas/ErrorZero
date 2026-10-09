package com.errorzero.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.errorzero.app.data.TagType
import com.errorzero.app.ui.components.Chip
import com.errorzero.app.ui.components.EmptyState
import com.errorzero.app.ui.components.Hairline
import com.errorzero.app.ui.components.MistakeRow
import com.errorzero.app.ui.components.PageHeader
import com.errorzero.app.ui.components.SectionLabel
import com.errorzero.app.ui.components.TextTab
import com.errorzero.app.ui.components.clickableNoRipple

@Composable
fun LibraryTab(vm: MainViewModel, padding: PaddingValues, onOpen: (Long) -> Unit) {
    val state by vm.library.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(filter.query.isNotEmpty()) }
    var showFilter by rememberSaveable { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 88.dp)) {
        item {
            PageHeader(
                title = "错题本",
                subtitle = "进行中 ${state.activeCount}  ·  已归档 ${state.archivedCount}",
            ) {
                IconButton(onClick = {
                    searching = !searching
                    if (!searching) vm.updateFilter { it.copy(query = "") }
                }) { Icon(if (searching) Icons.Outlined.Close else Icons.Outlined.Search, "搜索") }
            }
        }
        if (searching) {
            item {
                OutlinedTextField(
                    value = filter.query,
                    onValueChange = { q -> vm.updateFilter { it.copy(query = q) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    singleLine = true,
                    placeholder = { Text("搜索概要、备注、标签", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    shape = MaterialTheme.shapes.small,
                )
            }
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                item { TextTab("全部", filter.subjectId == null, { vm.updateFilter { it.copy(subjectId = null) } }) }
                items(subjects, key = { it.id }) { s ->
                    TextTab(s.name, filter.subjectId == s.id, { vm.updateFilter { it.copy(subjectId = s.id) } })
                }
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    SmallToggle("进行中", !filter.archived) { vm.updateFilter { it.copy(archived = false) } }
                    SmallToggle("已归档", filter.archived) { vm.updateFilter { it.copy(archived = true) } }
                }
                TextButton(onClick = { showFilter = true }) {
                    Icon(Icons.Outlined.FilterList, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (filter.tagFilterCount > 0) "  标签 ${filter.tagFilterCount}" else "  标签筛选",
                        color = if (filter.tagFilterCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Hairline(Modifier.padding(top = 4.dp))
        }

        if (!state.loading && state.items.isEmpty()) {
            item {
                when {
                    filter.query.isNotBlank() || filter.tagFilterCount > 0 -> EmptyState("没有匹配的题目", "试试调整搜索词或筛选条件")
                    filter.archived -> EmptyState("暂无归档", "掌握的题目可在详情页归档")
                    else -> EmptyState("还没有错题", "点击右下角记录第一道")
                }
            }
        }
        items(state.items, key = { it.mistake.id }) { item ->
            MistakeRow(item) { onOpen(item.mistake.id) }
            Hairline(Modifier.padding(start = 104.dp))
        }
    }

    if (showFilter) TagFilterSheet(vm) { showFilter = false }
}

@Composable
private fun SmallToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyMedium,
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickableNoRipple(onClick).padding(vertical = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TagFilterSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val filter by vm.filter.collectAsStateWithLifecycle()
    val tags by vm.tagUsage.collectAsStateWithLifecycle()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("标签筛选", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.updateFilter { it.copy(reasonTags = emptySet(), moduleTags = emptySet()) } }) {
                    Text("清除", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "同时满足所有选中的标签",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val reasons = tags.filter { it.type == TagType.REASON && it.count > 0 }
            val modules = tags.filter { it.type == TagType.MODULE && it.count > 0 }
            if (reasons.isEmpty() && modules.isEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("还没有使用过的标签", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (reasons.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionLabel("错因")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    reasons.forEach { t ->
                        val on = t.name in filter.reasonTags
                        Chip("${t.name}  ${t.count}", on, {
                            vm.updateFilter { f -> f.copy(reasonTags = if (on) f.reasonTags - t.name else f.reasonTags + t.name) }
                        })
                    }
                }
            }
            if (modules.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionLabel("模块")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    modules.forEach { t ->
                        val on = t.name in filter.moduleTags
                        Chip("${t.name}  ${t.count}", on, {
                            vm.updateFilter { f -> f.copy(moduleTags = if (on) f.moduleTags - t.name else f.moduleTags + t.name) }
                        })
                    }
                }
            }
        }
    }
}
