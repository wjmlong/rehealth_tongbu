package com.rehealth.genie.data

import android.util.Log
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.AuthenticatedApiClient
import com.rehealth.genie.network.dto.BehaviorRecordDto
import java.io.File
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BehaviorRecordRepository(
    private val apiClient: AuthenticatedApiClient,
) {
    suspend fun analyzeCameraPhoto(photoFile: File, occurredAt: Long = System.currentTimeMillis()): ApiResult<BehaviorRecordDto> =
        withContext(Dispatchers.IO) {
            val reduced = runCatching {
                CameraPhotoProcessor.awaitAndReducePhoto(photoFile)
            }
                .getOrElse {
                    Log.w(
                        TAG,
                        "Camera photo read failed: type=${it.javaClass.simpleName}, " +
                            "exists=${photoFile.exists()}, bytes=${photoFile.length()}",
                    )
                    runCatching { photoFile.delete() }
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
                runCatching { photoFile.delete() }
            }
        }

    suspend fun today(): ApiResult<List<BehaviorRecordDto>> {
        val offsetMinutes = OffsetDateTime.now().offset.totalSeconds / 60
        return apiClient.getTodayBehaviorRecords(LocalDate.now().toString(), offsetMinutes)
    }

    private companion object {
        const val TAG = "BehaviorPhoto"
    }
}
