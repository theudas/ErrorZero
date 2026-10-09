package com.errorzero.app.data

import androidx.room.withTransaction
import com.errorzero.app.review.ReviewSchedule
import com.errorzero.app.util.Dates
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class MistakeDraft(
    val id: Long,
    val subjectId: Long,
    val title: String,
    val note: String,
    val recordedAt: Long,
    val images: List<String>,
    val answerImages: List<String>,
    val reasonTags: List<String>,
    val moduleTags: List<String>,
)

data class ReviewResult(val next: Long?, val justCompleted: Boolean)

class Repository(private val db: AppDatabase, val images: ImageStore) {
    private val dao = db.dao()

    val subjects: Flow<List<Subject>> = dao.observeSubjects()
    val subjectCounts: Flow<List<SubjectCount>> = dao.observeSubjectCounts()
    val mistakes: Flow<List<MistakeFull>> = dao.observeMistakes()
    val tagUsage: Flow<List<TagUsage>> = dao.observeTagUsage()

    fun mistake(id: Long): Flow<MistakeFull?> = dao.observeMistake(id)
    suspend fun getMistake(id: Long): MistakeFull? = dao.getMistakeFull(id)
    suspend fun countDueToday(): Int = dao.countDue(Dates.endOfToday())

    // ---- 错题 ----

    suspend fun save(d: MistakeDraft): Long {
        val now = System.currentTimeMillis()
        var removed: List<String> = emptyList()
        val id = db.withTransaction {
            val id = if (d.id == 0L) {
                dao.insertMistake(
                    Mistake(
                        uuid = UUID.randomUUID().toString(),
                        subjectId = d.subjectId,
                        title = d.title.trim(),
                        note = d.note.trim(),
                        recordedAt = d.recordedAt,
                        createdAt = now,
                        updatedAt = now,
                        nextReviewAt = ReviewSchedule.first(now),
                    ),
                )
            } else {
                val old = dao.getMistake(d.id) ?: error("错题不存在")
                dao.updateMistake(
                    old.copy(
                        subjectId = d.subjectId,
                        title = d.title.trim(),
                        note = d.note.trim(),
                        recordedAt = d.recordedAt,
                        updatedAt = now,
                    ),
                )
                d.id
            }
            removed = dao.imageNames(id) - (d.images + d.answerImages).toSet()
            dao.deleteImagesOf(id)
            dao.insertImages(
                d.images.mapIndexed { i, name -> MistakeImage(mistakeId = id, fileName = name, sortOrder = i, kind = ImageKind.QUESTION) } +
                    d.answerImages.mapIndexed { i, name -> MistakeImage(mistakeId = id, fileName = name, sortOrder = i, kind = ImageKind.ANSWER) },
            )

            dao.clearTagsOf(id)
            val tagIds = d.reasonTags.map { tagId(TagType.REASON, it) } + d.moduleTags.map { tagId(TagType.MODULE, it) }
            dao.insertMistakeTags(tagIds.map { MistakeTag(id, it) })
            id
        }
        removed.forEach(images::delete)
        return id
    }

    private suspend fun tagId(type: Int, name: String): Long =
        dao.findTag(type, name)?.id ?: dao.insertTag(Tag(type = type, name = name))

    suspend fun recordReview(id: Long, mastered: Boolean): ReviewResult = db.withTransaction {
        val now = System.currentTimeMillis()
        val m = dao.getMistake(id) ?: return@withTransaction ReviewResult(null, false)
        val r = ReviewSchedule.afterReview(m.reviewStage, mastered, now)
        dao.updateMistake(
            m.copy(
                reviewStage = r.stage,
                nextReviewAt = r.next,
                reviewCount = m.reviewCount + 1,
                lastReviewedAt = now,
                updatedAt = now,
            ),
        )
        dao.insertReview(ReviewLog(mistakeId = id, reviewedAt = now, mastered = mastered))
        ReviewResult(r.next, r.justCompleted)
    }

    suspend fun setNextReview(id: Long, dayStart: Long) {
        val m = dao.getMistake(id) ?: return
        dao.updateMistake(m.copy(nextReviewAt = dayStart, updatedAt = System.currentTimeMillis()))
    }

    suspend fun setArchived(id: Long, archived: Boolean) {
        val m = dao.getMistake(id) ?: return
        val now = System.currentTimeMillis()
        val next = if (!archived && (m.nextReviewAt == null || m.nextReviewAt < Dates.startOfToday())) {
            Dates.startOfDayPlus(now, 1)
        } else {
            m.nextReviewAt
        }
        dao.updateMistake(
            m.copy(archived = archived, archivedAt = if (archived) now else null, nextReviewAt = next, updatedAt = now),
        )
    }

    suspend fun deleteMistake(id: Long) {
        val names = dao.imageNames(id)
        db.withTransaction {
            dao.clearTagsOf(id)
            dao.deleteImagesOf(id)
            dao.deleteReviewsOf(id)
            dao.deleteMistake(id)
        }
        names.forEach(images::delete)
    }

    // ---- 科目 ----

    /** 返回科目 id；同名科目已存在时直接返回已有的 */
    suspend fun addSubject(name: String): Long {
        val n = name.trim()
        dao.findSubject(n)?.let { return it.id }
        return dao.insertSubject(Subject(name = n, sortOrder = dao.maxSubjectOrder() + 1))
    }

    /** 重名时返回 false */
    suspend fun renameSubject(subject: Subject, name: String): Boolean {
        val n = name.trim()
        val existing = dao.findSubject(n)
        if (existing != null && existing.id != subject.id) return false
        dao.updateSubjects(listOf(subject.copy(name = n)))
        return true
    }

    /** 科目下仍有错题时不允许删除，返回该科目下的错题数（0 表示已删除） */
    suspend fun deleteSubject(id: Long): Int {
        val count = dao.countMistakesInSubject(id)
        if (count == 0) dao.deleteSubject(id)
        return count
    }

    suspend fun reorderSubjects(ordered: List<Subject>) {
        dao.updateSubjects(ordered.mapIndexed { i, s -> s.copy(sortOrder = i) })
    }

    // ---- 标签 ----

    /** 改成已存在的同类标签名时，两个标签合并 */
    suspend fun renameTag(tag: TagUsage, name: String) = db.withTransaction {
        val n = name.trim()
        val existing = dao.findTag(tag.type, n)
        if (existing != null && existing.id != tag.id) {
            dao.copyTagRefs(tag.id, existing.id)
            dao.deleteTagRefs(tag.id)
            dao.deleteTag(tag.id)
        } else {
            dao.renameTag(tag.id, n)
        }
    }

    suspend fun deleteTag(id: Long) = db.withTransaction {
        dao.deleteTagRefs(id)
        dao.deleteTag(id)
    }
}
