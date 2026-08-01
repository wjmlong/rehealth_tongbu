package com.rehealth.genie.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.delay

internal object CameraPhotoProcessor {
    suspend fun awaitAndReducePhoto(photoFile: File): ByteArray {
        awaitCameraWrite(photoFile)
        var lastFailure: RuntimeException? = null
        repeat(DECODE_RETRIES) { attempt ->
            try {
                return reducePhoto(photoFile)
            } catch (failure: RuntimeException) {
                lastFailure = failure
                if (attempt < DECODE_RETRIES - 1) delay(DECODE_RETRY_DELAY_MS)
            }
        }
        throw checkNotNull(lastFailure)
    }

    internal suspend fun awaitCameraWrite(photoFile: File) {
        var previousSize = -1L
        var stableSamples = 0
        repeat(PHOTO_READY_RETRIES) {
            val currentSize = if (photoFile.exists()) photoFile.length() else -1L
            stableSamples = if (currentSize > 0L && currentSize == previousSize) stableSamples + 1 else 0
            if (stableSamples >= REQUIRED_STABLE_SAMPLES) return
            previousSize = currentSize
            delay(PHOTO_READY_DELAY_MS)
        }
        require(photoFile.exists() && photoFile.length() > 0L) { "camera returned an empty photo" }
    }

    internal fun reducePhoto(photoFile: File): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(photoFile.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "photo dimensions are unavailable" }
        var sample = 1
        while (bounds.outWidth / sample > MAX_EDGE || bounds.outHeight / sample > MAX_EDGE) sample *= 2
        val bitmap = BitmapFactory.decodeFile(
            photoFile.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: error("photo cannot be decoded")
        val oriented = orient(bitmap, readOrientation(photoFile))
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

    private fun readOrientation(photoFile: File): Int =
        runCatching {
            ExifInterface(photoFile.absolutePath)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun orient(bitmap: Bitmap, orientation: Int): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            Matrix().apply { postRotate(degrees) },
            true,
        ).also { if (it !== bitmap) bitmap.recycle() }
    }

    private const val MAX_EDGE = 1600
    private const val MAX_UPLOAD_BYTES = 4 * 1024 * 1024
    private const val PHOTO_READY_RETRIES = 20
    private const val PHOTO_READY_DELAY_MS = 100L
    private const val REQUIRED_STABLE_SAMPLES = 3
    private const val DECODE_RETRIES = 3
    private const val DECODE_RETRY_DELAY_MS = 150L
}
