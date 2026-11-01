package com.jian.nemo.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jian.nemo.R
import java.io.File

object NotificationHelper {
    private const val CHANNEL_ID = "download_channel"
    private const val NOTIFICATION_ID = 1001

    private fun hasPostNotificationsPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun createNotificationChannel(context: Context) {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = appContext.getString(R.string.app_notification_channel_name)
            val descriptionText = appContext.getString(R.string.app_notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showProgress(context: Context, progress: Int, title: String? = null) {
        val appContext = context.applicationContext
        createNotificationChannel(appContext)

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle(title ?: appContext.getString(R.string.app_notification_download_progress))

            .setContentText(appContext.getString(R.string.app_notification_progress_format, progress))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .setOngoing(true)

        if (!hasPostNotificationsPermission(appContext)) return

        try {
            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showSuccess(context: Context, file: File, title: String? = null, content: String? = null) {
        val appContext = context.applicationContext
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                androidx.core.content.FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    file
                )
            } else {
                android.net.Uri.fromFile(file)
            }
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle(title ?: appContext.getString(R.string.app_notification_download_success))

            .setContentText(content ?: appContext.getString(R.string.app_notification_click_to_install))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .setProgress(0, 0, false)

        if (!hasPostNotificationsPermission(appContext)) return

        try {
            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID)
    }
}
