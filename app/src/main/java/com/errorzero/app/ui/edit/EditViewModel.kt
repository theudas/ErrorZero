package com.errorzero.app.ui.edit

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.errorzero.app.data.ImageKind
import com.errorzero.app.data.MistakeDraft
import com.errorzero.app.data.Repository
import com.errorzero.app.data.TagType
import com.errorzero.app.data.answerImages
import com.errorzero.app.data.moduleTags
import com.errorzero.app.data.questionImages
import com.errorzero.app.data.reasonTags
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EditState(
    val loaded: Boolean = false,
    val subjectId: Long? = null,
    val title: String = "",
    val note: String = "",
    val recordedAt: Long = System.currentTimeMillis(),
    val questionImages: List<String> = emptyList(),
    val answerImages: List<String> = emptyList(),
    val reasonTags: List<String> = emptyList(),
    val moduleTags: List<String> = emptyList(),
) {
    fun images(kind: Int) = if (kind == ImageKind.ANSWER) answerImages else questionImages
    fun withImages(kind: Int, list: List<String>) =
        if (kind == ImageKind.ANSWER) copy(answerImages = list) else copy(questionImages = list)
}

class EditViewModel(
    private val repo: Repository,
    private val mistakeId: Long,
    presetSubjectId: Long?,
) : ViewModel() {

    var state by mutableStateOf(EditState(subjectId = presetSubjectId, loaded = mistakeId == 0L))
        private set
    var processing by mutableStateOf(0)
        private set
    var message by mutableStateOf<String?>(null)
    var saving by mutableStateOf(false)
        private set

    private var initial: EditState? = if (mistakeId == 0L) state else null

    /** 本次编辑新产生、尚未保存的图片文件；放弃编辑时需删除 */
    private val newFiles = mutableSetOf<String>()
    private var saved = false

    val isNew get() = mistakeId == 0L
    val dirty get() = initial != null && state != initial

    val subjects = repo.subjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reasonHistory = repo.tagUsage.map { list -> list.filter { it.type == TagType.REASON }.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val moduleHistory = repo.tagUsage.map { list -> list.filter { it.type == TagType.MODULE }.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (mistakeId != 0L) {
            viewModelScope.launch {
                val m = repo.getMistake(mistakeId) ?: return@launch
                state = EditState(
                    loaded = true,
                    subjectId = m.mistake.subjectId,
                    title = m.mistake.title,
                    note = m.mistake.note,
                    recordedAt = m.mistake.recordedAt,
                    questionImages = m.questionImages.map { it.fileName },
                    answerImages = m.answerImages.map { it.fileName },
                    reasonTags = m.reasonTags,
                    moduleTags = m.moduleTags,
                )
                initial = state
            }
        }
    }

    fun update(transform: (EditState) -> EditState) {
        state = transform(state)
    }

    fun addSubject(name: String) = viewModelScope.launch {
        val id = repo.addSubject(name)
        state = state.copy(subjectId = id)
    }

    fun addImages(kind: Int, uris: List<Uri>) = viewModelScope.launch {
        processing++
        try {
            for (uri in uris) {
                runCatching { repo.images.import(uri) }
                    .onSuccess { name ->
                        newFiles += name
                        state = state.withImages(kind, state.images(kind) + name)
                    }
                    .onFailure { message = "图片读取失败：${it.message}" }
            }
        } finally {
            processing--
        }
    }

    /** 裁剪后替换原图，位置不变 */
    fun replaceImage(kind: Int, old: String, uri: Uri) = viewModelScope.launch {
        processing++
        try {
            runCatching { repo.images.import(uri) }
                .onSuccess { name ->
                    newFiles += name
                    state = state.withImages(kind, state.images(kind).map { if (it == old) name else it })
                    discardIfNew(old)
                }
                .onFailure { message = "图片处理失败：${it.message}" }
        } finally {
            processing--
        }
    }

    fun removeImage(kind: Int, name: String) {
        state = state.withImages(kind, state.images(kind) - name)
        discardIfNew(name)
    }

    fun moveFirst(kind: Int, name: String) {
        state = state.withImages(kind, listOf(name) + (state.images(kind) - name))
    }

    private fun discardIfNew(name: String) {
        if (newFiles.remove(name)) repo.images.delete(name)
    }

    fun save(onSaved: (Long) -> Unit) {
        val s = state
        val subjectId = s.subjectId
        when {
            subjectId == null -> message = "请选择科目"
            s.questionImages.isEmpty() && s.answerImages.isEmpty() && s.title.isBlank() && s.note.isBlank() ->
                message = "请至少添加一张图片或填写题目概要"
            processing > 0 -> message = "图片仍在处理中，请稍候"
            else -> viewModelScope.launch {
                saving = true
                try {
                    val id = repo.save(
                        MistakeDraft(
                            id = mistakeId,
                            subjectId = subjectId,
                            title = s.title,
                            note = s.note,
                            recordedAt = s.recordedAt,
                            images = s.questionImages,
                            answerImages = s.answerImages,
                            reasonTags = s.reasonTags,
                            moduleTags = s.moduleTags,
                        ),
                    )
                    saved = true
                    newFiles.clear()
                    onSaved(id)
                } catch (e: Exception) {
                    message = "保存失败：${e.message}"
                } finally {
                    saving = false
                }
            }
        }
    }

    override fun onCleared() {
        if (!saved) newFiles.forEach(repo.images::delete)
    }
}
