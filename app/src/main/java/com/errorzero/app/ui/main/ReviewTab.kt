package com.errorzero.app.ui.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.errorzero.app.review.ReviewSchedule
import com.errorzero.app.ui.components.EmptyState
import com.errorzero.app.ui.components.Hairline
import com.errorzero.app.ui.components.MistakeRow
import com.errorzero.app.ui.components.PageHeader
import com.errorzero.app.ui.components.SectionLabel
import com.errorzero.app.util.Dates

@Composable
fun ReviewTab(vm: MainViewModel, padding: PaddingValues, onOpen: (Long) -> Unit) {
    val state by vm.review.collectAsStateWithLifecycle()

    LazyColumn(contentPadding = padding) {
        item { PageHeader("重做", Dates.todayLabel()) }
        item {
            Row(Modifier.padding(horizontal = 24.dp)) {
                Text(
                    "${state.due.size}",
                    style = MaterialTheme.typography.displayMedium,
                    color = if (state.due.isNotEmpty()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alignByBaseline(),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "道题等待重做",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        item {
            Text(
                "按 ${ReviewSchedule.INTERVALS.joinToString(" / ")} 天间隔安排；掌握进入下一轮，不会则从头开始。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp),
            )
            Hairline()
        }

        if (!state.loading && state.due.isEmpty()) {
            item { EmptyState("今天没有待重做的题目", "保持节奏，明天再来") }
        }
        if (state.due.isNotEmpty()) {
            item { SectionLabel("今日", Modifier.padding(start = 24.dp, top = 20.dp, bottom = 4.dp)) }
            items(state.due, key = { "d${it.mistake.id}" }) { item ->
                MistakeRow(item) { onOpen(item.mistake.id) }
                Hairline(Modifier.padding(start = 104.dp))
            }
        }
        if (state.upcoming.isNotEmpty()) {
            item { SectionLabel("未来七天", Modifier.padding(start = 24.dp, top = 28.dp, bottom = 4.dp)) }
            items(state.upcoming, key = { "u${it.mistake.id}" }) { item ->
                MistakeRow(item) { onOpen(item.mistake.id) }
                Hairline(Modifier.padding(start = 104.dp))
            }
        }
    }
}
