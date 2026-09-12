package com.intranet.lunchorder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 提醒闹钟触发：发通知并注册下一天的闹钟。
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_LUNCH_REMINDER) return
        Notifications.show(context)
        ReminderScheduler.schedule(context)
    }

    companion object {
        const val ACTION_LUNCH_REMINDER = "com.intranet.lunchorder.ACTION_LUNCH_REMINDER"
    }
}
