package com.intranet.lunchorder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.intranet.lunchorder.data.prefs.PrefsStoreProvider
import com.intranet.lunchorder.logic.ReminderTime
import java.time.ZoneId

/**
 * 每日提醒闹钟调度：
 * - App 启动 / 提醒设置变更后调用 [schedule] 重新注册；
 * - 开关关闭时取消已注册闹钟；
 * - Android 12+ 检查 canScheduleExactAlarms()，未授权则降级 setAndAllowWhileIdle。
 */
object ReminderScheduler {

    private const val REQUEST_CODE = 1001

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_LUNCH_REMINDER)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** 当前是否允许精确闹钟（Android 12 以下恒为 true） */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    /** 按设置注册（或取消）每日提醒；返回是否使用了精确闹钟 */
    fun schedule(context: Context): Boolean {
        val prefs = PrefsStoreProvider.get(context)
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        val pi = pendingIntent(context)
        if (!prefs.notifyEnabled || !ReminderTime.isValid(prefs.notifyTime)) {
            am.cancel(pi)
            return false
        }
        val triggerAt = ReminderTime.nextTriggerMillis(
            System.currentTimeMillis(),
            prefs.notifyTime,
            ZoneId.systemDefault(),
        )
        val exact = canScheduleExact(context)
        if (exact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
        return exact
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pendingIntent(context))
    }
}
