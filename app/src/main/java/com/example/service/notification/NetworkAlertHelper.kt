package com.example.service.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.NetworkDevice

class NetworkAlertHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "netpulse_device_alerts"
        const val CHANNEL_NAME = "Network Device & Intrusion Alerts"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when unknown or suspicious devices join the local network"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun notifyNewDeviceDetected(device: NetworkDevice) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_ip", device.ip)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            device.ip.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚠️ Обнаружено новое подключение!")
            .setContentText("${device.ip} (${device.vendor}) подключился к Wi-Fi")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Новое устройство обнаружено в локальной сети!\n" +
                            "• IP: ${device.ip}\n" +
                            "• MAC: ${device.mac}\n" +
                            "• Производитель: ${device.vendor}\n" +
                            "• Тип: ${device.deviceType.displayName}\n" +
                            "Нажмите, чтобы просмотреть сведения или заблокировать."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(device.ip.hashCode(), notification)
        } catch (_: Exception) {}
    }
}
