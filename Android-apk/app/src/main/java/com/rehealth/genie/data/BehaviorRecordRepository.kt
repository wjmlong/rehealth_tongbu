package com.rehealth.genie.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.AuthenticatedApiClient
import com.rehealth.genie.network.dto.BehaviorRecordDto
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BehaviorRecordRepository(
    private val context: Context,
    private val apiClient: AuthenticatedApiClient,
) {
    suspend fun analyzeCameraPhoto(uri: Uri, occurredAt: Long = System.currentTimeMillis()): ApiResult<BehaviorRecordDto> =
        withContext(Dispatchers.IO) {
            val reduced = runCatching { reducePhoto(context.contentResolver, uri) }
                .getOrElse {
                    runCatching { context.contentResolver.delete(uri, null, null) }
                    return@withContext ApiResult.InvalidRequest("照片读取失败，请重新拍摄")
                }
            try {
                apiClient.analyzeBehaviorPhoto(
                    image = reduced,
                    contentType = "image/jpeg",
                    fileName = "behavior-${UUID.randomUUID()}.jpg",
                    requestId = UUID.randomUUID().toString(),
                    occurredAt = occurredAt,
                )
            } finally {
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
        }

    suspend fun today(): ApiResult<List<BehaviorRecordDto>> {
        val offsetMinutes = OffsetDateTime.now().offset.totalSeconds / 60
        return apiClient.getTodayBehaviorRecords(LocalDate.now().toString(), offsetMinutes)
    }

    internal fun reducePhoto(resolver: ContentResolver, uri: Uri): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: error("photo is unavailable")
        var sample = 1
        while (bounds.outWidth / sample > MAX_EDGE * 2 || bounds.outHeight / sample > MAX_EDGE * 2) sample *= 2
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("photo cannot be decoded")
        val oriented = orient(bitmap, readOrientation(resolver, uri))
        val scale = minOf(1f, MAX_EDGE.toFloat() / maxOf(oriented.width, oriented.height))
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                oriented,
                (oriented.width * scale).toInt().coerceAtLeast(1),
                (oriented.height * scale).toInt().coerceAtLeast(1),
                true,
            ).also { if (it !== oriented) oriented.recycle() }
        } else oriented
        return try {
            var quality = 86
            var bytes: ByteArray
            do {
                val output = ByteArrayOutputStream()
                check(scaled.compress(Bitmap.CompressFormat.JPEG, quality, output))
                bytes = output.toByteArray()
                quality -= 8
            } while (bytes.size > MAX_UPLOAD_BYTES && quality >= 54)
            require(bytes.size <= MAX_UPLOAD_BYTES) { "photo remains too large" }
            bytes
        } finally {
            scaled.recycle()
        }
    }

    private fun readOrientation(resolver: ContentResolver, uri: Uri): Int =
        runCatching {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun orient(bitmap: Bitmap, orientation: Int): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
            .also { if (it !== bitmap) bitmap.recycle() }
    }

    private companion object {
        const val MAX_EDGE = 1600
        const val MAX_UPLOAD_BYTES = 4 * 1024 * 1024
    }
}
