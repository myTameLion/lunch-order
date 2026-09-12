package com.intranet.lunchorder.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.intranet.lunchorder.R
import com.intranet.lunchorder.data.prefs.PrefsStoreProvider
import com.intranet.lunchorder.ui.MainActivity

/**
 * 通知渠道与提醒通知：channel id 固定 lunch_reminder。
 */
object Notifications {

    const val CHANNEL_ID = "lunch_reminder"
    const val NOTIFICATION_ID = 2001

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.reminder_channel_desc)
        }
        nm.createNotificationChannel(channel)
    }

    fun show(context: Context) {
        ensureChannel(context)
        if (!canNotify(context)) return

        // 点击通知打开 MainActivity（singleTask，已存在则复用）
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // 通知标题/内容优先使用管理员在中台配置的文案（D-014），缺省回退本地默认
        val prefs = PrefsStoreProvider.get(context)
        val title = prefs.notifyTitle.ifEmpty { context.getString(R.string.reminder_title) }
        val content = prefs.notifyContent.ifEmpty { context.getString(R.string.reminder_body) }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bowl)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /** Android 13+ 需要 POST_NOTIFICATIONS 运行时权限 */
    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private const val REQUEST_CODE_OPEN_APP = 2001
}
