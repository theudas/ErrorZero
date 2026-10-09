package com.errorzero.app.ui.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.errorzero.app.ErrorZeroApp
import com.errorzero.app.data.ImportMode
import com.errorzero.app.data.MistakeFull
import com.errorzero.app.data.ReminderConfig
import com.errorzero.app.data.moduleTags
import com.errorzero.app.data.reasonTags
import com.errorzero.app.review.Reminders
import com.errorzero.app.util.Dates
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryFilter(
    val subjectId: Long? = null,
    val archived: Boolean = false,
    val query: String = "",
    val reasonTags: Set<String> = emptySet(),
    val moduleTags: Set<String> = emptySet(),
) {
    val tagFilterCount get() = reasonTags.size + moduleTags.size
}

data class LibraryState(
    val loading: Boolean = true,
    val items: List<MistakeFull> = emptyList(),
    val activeCount: Int = 0,
    val archivedCount: Int = 0,
)

data class ReviewState(
    val loading: Boolean = true,
    val due: List<MistakeFull> = emptyList(),
    val upcoming: List<MistakeFull> = emptyList(),
)

class MainViewModel(private val app: ErrorZeroApp) : ViewModel() {
    private val repo = app.repository

    val subjects = repo.subjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tagUsage = repo.tagUsage.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reminder: StateFlow<ReminderConfig> = app.settings.reminder

    private val all = repo.mistakes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _filter = MutableStateFlow(LibraryFilter())
    val filter = _filter.asStateFlow()

    val library: StateFlow<LibraryState> = combine(all, _filter) { list, f ->
        if (list == null) return@combine LibraryState()
        val inSubject = list.filter { f.subjectId == null || it.mistake.subjectId == f.subjectId }
        val q = f.query.trim()
        LibraryState(
            loading = false,
            activeCount = inSubject.count { !it.mistake.archived },
            archivedCount = inSubject.count { it.mistake.archived },
            items = inSubject.filter { item ->
                item.mistake.archived == f.archived &&
                    item.reasonTags.containsAll(f.reasonTags) &&
                    item.moduleTags.containsAll(f.moduleTags) &&
                    (q.isEmpty() || matches(item, q))
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryState())

    val review: StateFlow<ReviewState> = all.map { list ->
        if (list == null) return@map ReviewState()
        val endToday = Dates.endOfToday()
        val endWeek = Dates.endOfDayAfter(7)
        val active = list.filter { !it.mistake.archived && it.mistake.nextReviewAt != null }
            .sortedBy { it.mistake.nextReviewAt }
        ReviewState(
            loading = false,
            due = active.filter { it.mistake.nextReviewAt!! <= endToday },
            upcoming = active.filter { it.mistake.nextReviewAt!! in (endToday + 1)..endWeek },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReviewState())

    private fun matches(item: MistakeFull, q: String): Boolean =
        item.mistake.title.contains(q, true) ||
            item.mistake.note.contains(q, true) ||
            item.subject?.name?.contains(q, true) == true ||
            item.tags.any { it.name.contains(q, true) }

    fun updateFilter(transform: (LibraryFilter) -> LibraryFilter) {
        _filter.value = transform(_filter.value)
    }

    // ---- 设置 ----

    fun setReminder(config: ReminderConfig) {
        app.settings.setReminder(config)
        Reminders.schedule(app, config, replace = true)
    }

    private val _busy = MutableStateFlow<String?>(null)
    val busy = _busy.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun export(target: Uri) = viewModelScope.launch {
        _busy.value = "正在导出…"
        runCatching { app.backup.export(target) }
            .onSuccess { _messages.send("已导出 $it 道错题") }
            .onFailure { _messages.send("导出失败：${it.message}") }
        _busy.value = null
    }

    fun import(source: Uri, mode: ImportMode) = viewModelScope.launch {
        _busy.value = "正在导入…"
        runCatching { app.backup.import(source, mode) }
            .onSuccess { r ->
                _messages.send("导入完成：新增 ${r.added} 道" + if (r.skipped > 0) "，跳过重复 ${r.skipped} 道" else "")
                _filter.value = LibraryFilter()
            }
            .onFailure { _messages.send("导入失败：${it.message}") }
        _busy.value = null
    }
}
