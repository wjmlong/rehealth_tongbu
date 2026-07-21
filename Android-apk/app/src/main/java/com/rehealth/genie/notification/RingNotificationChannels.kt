package com.rehealth.genie.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rehealth.genie.service.RingForegroundService

object RingNotificationChannels {
    const val COLLECTION_CHANNEL_ID = "ring_collection"
    const val COLLECTION_NOTIFICATION_ID = 3001

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            COLLECTION_CHANNEL_ID,
            "Ring collection",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows when ReHealth is collecting ring data locally."
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun collectionNotification(
        context: Context,
        contentText: String,
    ): Notification {
        ensure(context)
        val stopIntent = PendingIntent.getService(
            context,
            10,
            RingForegroundService.intent(context, RingForegroundService.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, COLLECTION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("ReHealth ring collection")
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .build()
    }
}
