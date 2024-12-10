package hr.foi.rampu.memento.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import hr.foi.rampu.memento.NewsActivity
import hr.foi.rampu.memento.R

class FCMService : FirebaseMessagingService() {
    private var id = 0

    override fun onCreate() {
        FirebaseMessaging.getInstance().subscribeToTopic("news")

        val channelNews = NotificationChannel("news", "News channel", NotificationManager.IMPORTANCE_HIGH)
        val channelInfo = NotificationChannel("info", "Info Channel", NotificationManager.IMPORTANCE_HIGH)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channelNews)
        notificationManager.createNotificationChannel(channelInfo)
    }

    @SuppressLint("MissingPermission")
    override fun onMessageReceived(message: RemoteMessage) {
        Log.i("MEMENTO_FCM_MSG", "Message data payload: ${message.data}")
        lateinit var notification: Notification
        val msgPayload = message.data["payload"]

        if (msgPayload == "news") {
            val intentShow = Intent(this, NewsActivity::class.java).apply {
                putExtra("news_name", message.data["newNewsName"])
            }

            val openActivity =
                PendingIntent.getActivity(this, 0, intentShow, PendingIntent.FLAG_IMMUTABLE)

            notification =
                NotificationCompat.Builder(applicationContext, "news")
                    .setContentTitle(message.data["newNewsName"])
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message.data["newsText"]))
                    .setSmallIcon(R.drawable.baseline_wysiwyg_24)
                    .setContentIntent(openActivity)
                    .setAutoCancel(true)
                    .build()
        } else {
            notification = NotificationCompat.Builder(applicationContext, "info")
                .setContentTitle(message.data["infoTitle"])
                .setStyle(NotificationCompat.BigTextStyle().bigText(message.data["infoText"]))
                    .setSmallIcon(R.drawable.ic_baseline_info_24)
                .build()
        }

        with(NotificationManagerCompat.from(this)) {
            notify(++id, notification)
        }

    }
}