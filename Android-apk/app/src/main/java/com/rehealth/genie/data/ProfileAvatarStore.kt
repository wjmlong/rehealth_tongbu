package com.rehealth.genie.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.roundToInt

internal fun profileAvatarStorageKey(identity: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(identity.trim().toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
        .take(24)

class ProfileAvatarStore(
    context: Context,
    identity: String,
) {
    private val appContext = context.applicationContext
    private val storageKey = profileAvatarStorageKey(identity)
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val avatarDirectory = File(appContext.filesDir, AVATAR_DIRECTORY)
    private val avatarFile = File(avatarDirectory, "$storageKey.jpg")

    fun load(): Bitmap? {
        val storedName = preferences.getString(preferenceKey(), null) ?: return null
        if (storedName != avatarFile.name || !avatarFile.isFile) return null
        return BitmapFactory.decodeFile(avatarFile.absolutePath)
    }

    fun save(source: Uri): Bitmap {
        val bitmap = decodeScaledBitmap(source)
            ?: throw IllegalArgumentException("无法读取所选图片")
        avatarDirectory.mkdirs()
        val temporary = File(avatarDirectory, "$storageKey.tmp")
        FileOutputStream(temporary).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                "无法保存头像"
            }
        }
        if (avatarFile.exists() && !avatarFile.delete()) {
            temporary.delete()
            throw IllegalStateException("无法替换本机头像")
        }
        if (!temporary.renameTo(avatarFile)) {
            temporary.delete()
            throw IllegalStateException("无法完成头像保存")
        }
        preferences.edit().putString(preferenceKey(), avatarFile.name).apply()
        return BitmapFactory.decodeFile(avatarFile.absolutePath)
            ?: throw IllegalStateException("无法读取已保存头像")
    }

    private fun decodeScaledBitmap(source: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val imageSource = ImageDecoder.createSource(appContext.contentResolver, source)
            ImageDecoder.decodeBitmap(imageSource) { decoder, info, _ ->
                val width = info.size.width
                val height = info.size.height
                val longest = maxOf(width, height)
                if (longest > MAX_AVATAR_DIMENSION) {
                    val scale = MAX_AVATAR_DIMENSION.toFloat() / longest
                    decoder.setTargetSize(
                        (width * scale).roundToInt().coerceAtLeast(1),
                        (height * scale).roundToInt().coerceAtLeast(1),
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            appContext.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sampleSize = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_AVATAR_DIMENSION) {
                sampleSize *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            appContext.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        }
    }

    private fun preferenceKey(): String = "avatar_file_$storageKey"

    private companion object {
        const val PREFERENCES_NAME = "rehealth_profile_avatars"
        const val AVATAR_DIRECTORY = "profile_avatars"
        const val MAX_AVATAR_DIMENSION = 1_024
        const val JPEG_QUALITY = 90
    }
}
