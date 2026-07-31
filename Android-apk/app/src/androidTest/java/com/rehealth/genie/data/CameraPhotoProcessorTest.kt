package com.rehealth.genie.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraPhotoProcessorTest {
    @Test
    fun waitsForDelayedCameraWriteAndProducesBoundedJpeg() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val photoFile = File.createTempFile("delayed-camera-", ".jpg", context.cacheDir)
        try {
            launch {
                delay(250)
                val bitmap = Bitmap.createBitmap(2400, 1800, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(Color.rgb(42, 170, 126))
                }
                FileOutputStream(photoFile).use { output ->
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output))
                }
                bitmap.recycle()
            }

            val reduced = CameraPhotoProcessor.awaitAndReducePhoto(photoFile)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(reduced, 0, reduced.size, bounds)

            assertTrue(reduced.isNotEmpty())
            assertTrue(reduced.size <= 4 * 1024 * 1024)
            assertTrue(bounds.outWidth <= 1600)
            assertTrue(bounds.outHeight <= 1600)
        } finally {
            photoFile.delete()
        }
    }
}
