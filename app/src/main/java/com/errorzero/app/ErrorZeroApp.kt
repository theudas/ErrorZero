package com.errorzero.app

import android.app.Application
import android.content.Context
import com.errorzero.app.data.AppDatabase
import com.errorzero.app.data.AppSettings
import com.errorzero.app.data.BackupManager
import com.errorzero.app.data.ImageStore
import com.errorzero.app.data.Repository
import com.errorzero.app.review.Reminders

class ErrorZeroApp : Application() {
    lateinit var repository: Repository
        private set
    lateinit var settings: AppSettings
        private set
    lateinit var backup: BackupManager
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.build(this)
        val images = ImageStore(this)
        repository = Repository(db, images)
        settings = AppSettings(this)
        backup = BackupManager(this, db, images)
        images.clearCameraCache()
        Reminders.createChannel(this)
        Reminders.schedule(this, settings.reminder.value, replace = false)
    }
}

val Context.app: ErrorZeroApp get() = applicationContext as ErrorZeroApp
