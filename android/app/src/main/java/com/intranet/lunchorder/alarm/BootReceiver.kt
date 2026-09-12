package com.intranet.lunchorder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 开机完成：重新注册每日提醒（未开启提醒时 schedule 内部会自动取消/跳过）。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            ReminderScheduler.schedule(context)
        }
    }
}
