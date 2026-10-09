package com.errorzero.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/** 错题图片保存在 filesDir/images 下，数据库只记录文件名 */
class ImageStore(private val context: Context) {
    val dir: File = File(context.filesDir, "images").apply { mkdirs() }
    private val cameraDir: File get() = File(context.cacheDir, "camera").apply { mkdirs() }

    fun file(name: String): File = File(dir, name)

    fun delete(name: String) {
        File(dir, name).delete()
    }

    fun newCameraUri(): Uri {
        val f = File(cameraDir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
    }

    fun clearCameraCache() {
        cameraDir.listFiles()?.forEach { it.delete() }
    }

    /** 读取任意来源的图片，纠正方向、限制尺寸后保存为 JPEG，返回文件名 */
    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("无法读取图片")

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IOException("无法读取图片")

        val orientation = runCatching {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        bitmap = applyOrientation(bitmap, orientation)

        val longSide = max(bitmap.width, bitmap.height)
        if (longSide > MAX_SIDE) {
            val scale = MAX_SIDE.toFloat() / longSide
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).roundToInt(),
                (bitmap.height * scale).roundToInt(),
                true,
            )
            if (scaled != bitmap) bitmap.recycle()
            bitmap = scaled
        }

        val name = "${UUID.randomUUID()}.jpg"
        FileOutputStream(File(dir, name)).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bitmap.recycle()
        name
    }

    private fun applyOrientation(src: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
            else -> return src
        }
        val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        if (out != src) src.recycle()
        return out
    }

    companion object {
        private const val MAX_SIDE = 2400
    }
}
