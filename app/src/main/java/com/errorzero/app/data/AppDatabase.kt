package com.errorzero.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Subject::class, Mistake::class, MistakeImage::class, Tag::class, MistakeTag::class, ReviewLog::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        val DEFAULT_SUBJECTS = listOf("语文", "数学", "英语", "物理", "化学", "生物", "政治", "历史", "地理")

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "errorzero.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        DEFAULT_SUBJECTS.forEachIndexed { i, name ->
                            db.execSQL("INSERT INTO subjects(name, sortOrder) VALUES(?, ?)", arrayOf<Any>(name, i))
                        }
                    }
                })
                .build()
    }
}
