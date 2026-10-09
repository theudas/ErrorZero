package com.errorzero.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.errorzero.app.data.MistakeFull
import com.errorzero.app.data.Repository
import com.errorzero.app.data.ReviewResult
import com.errorzero.app.util.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class Loaded(val loaded: Boolean, val item: MistakeFull?)

class DetailViewModel(private val repo: Repository, private val id: Long) : ViewModel() {
    val item = repo.mistake(id).map { Loaded(true, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Loaded(false, null))

    private var deleting = false

    fun review(mastered: Boolean, onResult: (ReviewResult) -> Unit) = viewModelScope.launch {
        onResult(repo.recordReview(id, mastered))
    }

    fun setNextReview(date: LocalDate) = viewModelScope.launch {
        repo.setNextReview(id, Dates.startOfDay(date))
    }

    fun setArchived(archived: Boolean) = viewModelScope.launch {
        repo.setArchived(id, archived)
    }

    /** 删除后 item 变为 null，界面据此返回 */
    fun delete() {
        if (deleting) return
        deleting = true
        viewModelScope.launch { repo.deleteMistake(id) }
    }
}
