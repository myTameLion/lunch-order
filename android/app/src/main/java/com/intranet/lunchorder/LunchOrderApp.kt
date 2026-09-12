package com.intranet.lunchorder

import android.app.Application
import com.intranet.lunchorder.alarm.ReminderScheduler

/**
 * Application：启动时按当前设置重新注册每日提醒。
 */
class LunchOrderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.schedule(this)
    }
}
