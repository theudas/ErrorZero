package com.errorzero.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "subjects", indices = [Index(value = ["name"], unique = true)])
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "mistakes",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("subjectId"), Index(value = ["uuid"], unique = true), Index("nextReviewAt")],
)
data class Mistake(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 跨设备稳定标识，用于导入合并时去重 */
    val uuid: String,
    val subjectId: Long,
    val title: String,
    val note: String,
    /** 用户可调整的记录时间 */
    val recordedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val archived: Boolean = false,
    val archivedAt: Long? = null,
    /** 已完成的复习轮次，0 表示尚未开始；达到 ReviewSchedule.ROUNDS 表示全部完成 */
    val reviewStage: Int = 0,
    /** 下次重做日期（当天零点）；为 null 表示不提醒 */
    val nextReviewAt: Long?,
    val reviewCount: Int = 0,
    val lastReviewedAt: Long? = null,
)

@Entity(
    tableName = "images",
    foreignKeys = [
        ForeignKey(
            entity = Mistake::class,
            parentColumns = ["id"],
            childColumns = ["mistakeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mistakeId")],
)
data class MistakeImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mistakeId: Long,
    val fileName: String,
    val sortOrder: Int,
    /** ImageKind.QUESTION 题目 / ImageKind.ANSWER 答案 */
    val kind: Int = ImageKind.QUESTION,
)

object ImageKind {
    const val QUESTION = 0
    const val ANSWER = 1
}

object TagType {
    const val REASON = 0
    const val MODULE = 1
}

@Entity(tableName = "tags", indices = [Index(value = ["type", "name"], unique = true)])
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: Int,
    val name: String,
)

@Entity(
    tableName = "mistake_tags",
    primaryKeys = ["mistakeId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Mistake::class,
            parentColumns = ["id"],
            childColumns = ["mistakeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId")],
)
data class MistakeTag(val mistakeId: Long, val tagId: Long)

@Entity(
    tableName = "reviews",
    foreignKeys = [
        ForeignKey(
            entity = Mistake::class,
            parentColumns = ["id"],
            childColumns = ["mistakeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mistakeId")],
)
data class ReviewLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mistakeId: Long,
    val reviewedAt: Long,
    val mastered: Boolean,
)

data class MistakeFull(
    @Embedded val mistake: Mistake,
    @Relation(parentColumn = "subjectId", entityColumn = "id")
    val subject: Subject?,
    @Relation(parentColumn = "id", entityColumn = "mistakeId")
    val images: List<MistakeImage>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(MistakeTag::class, parentColumn = "mistakeId", entityColumn = "tagId"),
    )
    val tags: List<Tag>,
    @Relation(parentColumn = "id", entityColumn = "mistakeId")
    val reviews: List<ReviewLog>,
)

val MistakeFull.sortedImages: List<MistakeImage> get() = images.sortedBy { it.sortOrder }
val MistakeFull.questionImages: List<MistakeImage> get() = sortedImages.filter { it.kind == ImageKind.QUESTION }
val MistakeFull.answerImages: List<MistakeImage> get() = sortedImages.filter { it.kind == ImageKind.ANSWER }
fun MistakeFull.imagesOf(kind: Int): List<MistakeImage> = sortedImages.filter { it.kind == kind }
val MistakeFull.reasonTags: List<String> get() = tags.filter { it.type == TagType.REASON }.map { it.name }.sorted()
val MistakeFull.moduleTags: List<String> get() = tags.filter { it.type == TagType.MODULE }.map { it.name }.sorted()

val MistakeFull.displayTitle: String
    get() = mistake.title.ifBlank {
        mistake.note.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: "未填写概要"
    }

data class TagUsage(val id: Long, val type: Int, val name: String, val count: Int)

data class SubjectCount(val subjectId: Long, val count: Int)
