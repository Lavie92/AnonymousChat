package com.example.anonymousChat.notification

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.anonymousChat.R
import com.example.anonymousChat.presentation.ChatActivity
import com.example.anonymousChat.presentation.ChatNearestActivity

object NotificationUtils {
    private const val CHANNEL_ID = "ChatChannel"
    private const val NOTIFICATION_ID = 1
    fun showNotification(context: Context, title: String, content: String, chatType: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Chat Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val activity: Activity = if (chatType == "chatRooms") {
            ChatActivity()
        } else {
            ChatNearestActivity()
        }

        val intent = Intent(context, activity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
