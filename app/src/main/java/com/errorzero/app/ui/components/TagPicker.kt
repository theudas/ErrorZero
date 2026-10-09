package com.errorzero.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

private const val COLLAPSED_COUNT = 12

/**
 * 多选标签：可输入新标签（回车或逗号分隔），也可从历史标签中点选。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagPicker(
    selected: List<String>,
    history: List<String>,
    placeholder: String,
    onChange: (List<String>) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }

    fun commit() {
        val parts = input.split(',', '，', '、', ';', '；').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isNotEmpty()) onChange((selected + parts).distinct())
        input = ""
    }

    Column(Modifier.fillMaxWidth()) {
        if (selected.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                selected.forEach { tag ->
                    Chip(tag, selected = true, trailingClose = true, onClick = { onChange(selected - tag) })
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = input,
            onValueChange = { v ->
                if (v.endsWith(",") || v.endsWith("，")) {
                    input = v
                    commit()
                } else {
                    input = v
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            shape = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            trailingIcon = {
                if (input.isNotBlank()) TextButton(onClick = { commit() }) { Text("添加") }
            },
        )

        val keyword = input.trim()
        val candidates = history.filter { it !in selected && (keyword.isEmpty() || it.contains(keyword, ignoreCase = true)) }
        if (candidates.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            SectionLabel("历史标签")
            Spacer(Modifier.height(8.dp))
            val shown = if (expanded) candidates else candidates.take(COLLAPSED_COUNT)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shown.forEach { tag ->
                    Chip(tag, selected = false, onClick = { onChange(selected + tag) })
                }
                if (candidates.size > COLLAPSED_COUNT) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(
                            if (expanded) "收起" else "更多 ${candidates.size - COLLAPSED_COUNT}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
