package com.kaizen.khushu.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kaizen.khushu.notifications.PrayerNotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PrayerRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                PrayerNotificationScheduler(context).syncNotifications()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
