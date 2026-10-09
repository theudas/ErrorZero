package com.errorzero.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long,
    val subjects: List<BackupSubject>,
    val tags: List<BackupTag>,
    val mistakes: List<BackupMistake>,
) {
    companion object {
        const val FORMAT = "errorzero-backup"
        const val VERSION = 1
    }
}

@Serializable
data class BackupSubject(val name: String, val sortOrder: Int)

@Serializable
data class BackupTag(val type: Int, val name: String)

@Serializable
data class BackupReview(val reviewedAt: Long, val mastered: Boolean)

@Serializable
data class BackupMistake(
    val uuid: String,
    val subject: String,
    val title: String = "",
    val note: String = "",
    val recordedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val archived: Boolean = false,
    val archivedAt: Long? = null,
    val reviewStage: Int = 0,
    val nextReviewAt: Long? = null,
    val reviewCount: Int = 0,
    val lastReviewedAt: Long? = null,
    val images: List<String> = emptyList(),
    val answerImages: List<String> = emptyList(),
    val reasonTags: List<String> = emptyList(),
    val moduleTags: List<String> = emptyList(),
    val reviews: List<BackupReview> = emptyList(),
)

enum class ImportMode { MERGE, REPLACE }

data class ImportResult(val added: Int, val skipped: Int)

/**
 * 备份文件为 zip：
 *   data.json        全部结构化数据
 *   images/<name>    题目与答案图片
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val images: ImageStore,
) {
    private val dao = db.dao()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun export(target: Uri): Int = withContext(Dispatchers.IO) {
        val subjects = dao.getSubjects()
        val subjectNames = subjects.associate { it.id to it.name }
        val all = dao.getAllMistakes()
        val backup = BackupFile(
            exportedAt = System.currentTimeMillis(),
            subjects = subjects.map { BackupSubject(it.name, it.sortOrder) },
            tags = dao.getTags().map { BackupTag(it.type, it.name) },
            mistakes = all.map { f ->
                val m = f.mistake
                BackupMistake(
                    uuid = m.uuid,
                    subject = subjectNames[m.subjectId] ?: "未分类",
                    title = m.title,
                    note = m.note,
                    recordedAt = m.recordedAt,
                    createdAt = m.createdAt,
                    updatedAt = m.updatedAt,
                    archived = m.archived,
                    archivedAt = m.archivedAt,
                    reviewStage = m.reviewStage,
                    nextReviewAt = m.nextReviewAt,
                    reviewCount = m.reviewCount,
                    lastReviewedAt = m.lastReviewedAt,
                    images = f.questionImages.map { it.fileName },
                    answerImages = f.answerImages.map { it.fileName },
                    reasonTags = f.reasonTags,
                    moduleTags = f.moduleTags,
                    reviews = f.reviews.sortedBy { it.reviewedAt }.map { BackupReview(it.reviewedAt, it.mastered) },
                )
            },
        )

        val out = context.contentResolver.openOutputStream(target, "wt")
            ?: throw IOException("无法写入目标文件")
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            zip.putNextEntry(ZipEntry(DATA_ENTRY))
            zip.write(json.encodeToString(backup).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for (name in backup.mistakes.flatMap { it.images + it.answerImages }.distinct()) {
                val f = images.file(name)
                if (!f.exists()) continue
                zip.putNextEntry(ZipEntry("$IMAGE_DIR$name"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        all.size
    }

    suspend fun import(source: Uri, mode: ImportMode): ImportResult = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, "import").apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            var dataBytes: ByteArray? = null
            val input = context.contentResolver.openInputStream(source)
                ?: throw IOException("无法读取文件")
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    when {
                        entry.isDirectory -> Unit
                        name == DATA_ENTRY -> dataBytes = zip.readBytes()
                        name.startsWith(IMAGE_DIR) -> {
                            val fileName = name.removePrefix(IMAGE_DIR)
                            if (isSafeName(fileName)) File(tmp, fileName).outputStream().use { zip.copyTo(it) }
                        }
                    }
                    zip.closeEntry()
                }
            }
            val bytes = dataBytes ?: throw IOException("不是有效的 ErrorZero 备份文件")
            val backup = runCatching { json.decodeFromString<BackupFile>(bytes.toString(Charsets.UTF_8)) }
                .getOrElse { throw IOException("备份文件已损坏") }
            if (backup.format != BackupFile.FORMAT) throw IOException("不是有效的 ErrorZero 备份文件")
            if (backup.version > BackupFile.VERSION) throw IOException("备份来自更新版本的应用，请先升级")

            // 先落地图片，再写数据库：即使中途失败也不会出现数据库引用缺失文件的情况
            tmp.listFiles()?.forEach { f ->
                val dest = images.file(f.name)
                if (!dest.exists()) f.copyTo(dest)
            }

            var added = 0
            var skipped = 0
            db.withTransaction {
                if (mode == ImportMode.REPLACE) {
                    dao.wipeMistakeTags()
                    dao.wipeImages()
                    dao.wipeReviews()
                    dao.wipeMistakes()
                    dao.wipeTags()
                    dao.wipeSubjects()
                }
                val subjectIds = HashMap<String, Long>()
                suspend fun subjectId(name: String): Long = subjectIds[name]
                    ?: (dao.findSubject(name)?.id ?: dao.insertSubject(Subject(name = name, sortOrder = dao.maxSubjectOrder() + 1)))
                        .also { subjectIds[name] = it }

                val tagIds = HashMap<Pair<Int, String>, Long>()
                suspend fun tagId(type: Int, name: String): Long = tagIds[type to name]
                    ?: (dao.findTag(type, name)?.id ?: dao.insertTag(Tag(type = type, name = name)))
                        .also { tagIds[type to name] = it }

                backup.subjects.sortedBy { it.sortOrder }.forEach { subjectId(it.name) }
                backup.tags.forEach { tagId(it.type, it.name) }

                for (b in backup.mistakes) {
                    if (dao.uuidExists(b.uuid)) {
                        skipped++
                        continue
                    }
                    val id = dao.insertMistake(
                        Mistake(
                            uuid = b.uuid,
                            subjectId = subjectId(b.subject),
                            title = b.title,
                            note = b.note,
                            recordedAt = b.recordedAt,
                            createdAt = b.createdAt,
                            updatedAt = b.updatedAt,
                            archived = b.archived,
                            archivedAt = b.archivedAt,
                            reviewStage = b.reviewStage,
                            nextReviewAt = b.nextReviewAt,
                            reviewCount = b.reviewCount,
                            lastReviewedAt = b.lastReviewedAt,
                        ),
                    )
                    dao.insertImages(
                        b.images.filter { images.file(it).exists() }
                            .mapIndexed { i, n -> MistakeImage(mistakeId = id, fileName = n, sortOrder = i, kind = ImageKind.QUESTION) } +
                            b.answerImages.filter { images.file(it).exists() }
                                .mapIndexed { i, n -> MistakeImage(mistakeId = id, fileName = n, sortOrder = i, kind = ImageKind.ANSWER) },
                    )
                    val refs = b.reasonTags.map { tagId(TagType.REASON, it) } + b.moduleTags.map { tagId(TagType.MODULE, it) }
                    dao.insertMistakeTags(refs.map { MistakeTag(id, it) })
                    dao.insertReviews(b.reviews.map { ReviewLog(mistakeId = id, reviewedAt = it.reviewedAt, mastered = it.mastered) })
                    added++
                }
            }
            removeOrphanImages()
            ImportResult(added, skipped)
        } finally {
            tmp.deleteRecursively()
        }
    }

    private suspend fun removeOrphanImages() {
        val referenced = dao.allImageNames().toHashSet()
        images.dir.listFiles()?.forEach { if (it.name !in referenced) it.delete() }
    }

    private fun isSafeName(name: String) =
        name.isNotEmpty() && !name.contains('/') && !name.contains('\\') && name != "." && name != ".."

    companion object {
        private const val DATA_ENTRY = "data.json"
        private const val IMAGE_DIR = "images/"
    }
}
