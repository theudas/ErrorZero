package com.errorzero.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // ---- 科目 ----

    @Query("SELECT * FROM subjects ORDER BY sortOrder, id")
    fun observeSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects ORDER BY sortOrder, id")
    suspend fun getSubjects(): List<Subject>

    @Query("SELECT * FROM subjects WHERE name = :name LIMIT 1")
    suspend fun findSubject(name: String): Subject?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM subjects")
    suspend fun maxSubjectOrder(): Int

    @Insert
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubjects(subjects: List<Subject>)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubject(id: Long)

    @Query("SELECT subjectId, COUNT(*) AS count FROM mistakes GROUP BY subjectId")
    fun observeSubjectCounts(): Flow<List<SubjectCount>>

    @Query("SELECT COUNT(*) FROM mistakes WHERE subjectId = :id")
    suspend fun countMistakesInSubject(id: Long): Int

    // ---- 错题 ----

    @Transaction
    @Query("SELECT * FROM mistakes ORDER BY recordedAt DESC")
    fun observeMistakes(): Flow<List<MistakeFull>>

    @Transaction
    @Query("SELECT * FROM mistakes WHERE id = :id")
    fun observeMistake(id: Long): Flow<MistakeFull?>

    @Transaction
    @Query("SELECT * FROM mistakes WHERE id = :id")
    suspend fun getMistakeFull(id: Long): MistakeFull?

    @Transaction
    @Query("SELECT * FROM mistakes ORDER BY recordedAt")
    suspend fun getAllMistakes(): List<MistakeFull>

    @Query("SELECT * FROM mistakes WHERE id = :id")
    suspend fun getMistake(id: Long): Mistake?

    @Query("SELECT EXISTS(SELECT 1 FROM mistakes WHERE uuid = :uuid)")
    suspend fun uuidExists(uuid: String): Boolean

    @Query("SELECT COUNT(*) FROM mistakes WHERE archived = 0 AND nextReviewAt IS NOT NULL AND nextReviewAt <= :until")
    suspend fun countDue(until: Long): Int

    @Insert
    suspend fun insertMistake(mistake: Mistake): Long

    @Update
    suspend fun updateMistake(mistake: Mistake)

    @Query("DELETE FROM mistakes WHERE id = :id")
    suspend fun deleteMistake(id: Long)

    // ---- 图片 ----

    @Query("SELECT fileName FROM images WHERE mistakeId = :id")
    suspend fun imageNames(id: Long): List<String>

    @Query("SELECT fileName FROM images")
    suspend fun allImageNames(): List<String>

    @Query("DELETE FROM images WHERE mistakeId = :id")
    suspend fun deleteImagesOf(id: Long)

    @Insert
    suspend fun insertImages(images: List<MistakeImage>)

    // ---- 标签 ----

    @Query(
        """
        SELECT t.id AS id, t.type AS type, t.name AS name, COUNT(mt.mistakeId) AS count
        FROM tags t LEFT JOIN mistake_tags mt ON mt.tagId = t.id
        GROUP BY t.id
        ORDER BY count DESC, t.name
        """,
    )
    fun observeTagUsage(): Flow<List<TagUsage>>

    @Query("SELECT * FROM tags")
    suspend fun getTags(): List<Tag>

    @Query("SELECT * FROM tags WHERE type = :type AND name = :name LIMIT 1")
    suspend fun findTag(type: Int, name: String): Tag?

    @Insert
    suspend fun insertTag(tag: Tag): Long

    @Query("UPDATE tags SET name = :name WHERE id = :id")
    suspend fun renameTag(id: Long, name: String)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    @Query("INSERT OR IGNORE INTO mistake_tags(mistakeId, tagId) SELECT mistakeId, :to FROM mistake_tags WHERE tagId = :from")
    suspend fun copyTagRefs(from: Long, to: Long)

    @Query("DELETE FROM mistake_tags WHERE tagId = :tagId")
    suspend fun deleteTagRefs(tagId: Long)

    @Query("DELETE FROM mistake_tags WHERE mistakeId = :id")
    suspend fun clearTagsOf(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMistakeTags(refs: List<MistakeTag>)

    // ---- 复习记录 ----

    @Insert
    suspend fun insertReview(review: ReviewLog)

    @Insert
    suspend fun insertReviews(reviews: List<ReviewLog>)

    @Query("DELETE FROM reviews WHERE mistakeId = :id")
    suspend fun deleteReviewsOf(id: Long)

    // ---- 清空（覆盖导入用） ----

    @Query("DELETE FROM mistake_tags")
    suspend fun wipeMistakeTags()

    @Query("DELETE FROM images")
    suspend fun wipeImages()

    @Query("DELETE FROM reviews")
    suspend fun wipeReviews()

    @Query("DELETE FROM mistakes")
    suspend fun wipeMistakes()

    @Query("DELETE FROM tags")
    suspend fun wipeTags()

    @Query("DELETE FROM subjects")
    suspend fun wipeSubjects()
}
