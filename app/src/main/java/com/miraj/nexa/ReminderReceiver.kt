package com.miraj.nexa

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("msg") ?: "তোমার reminder"
        val id = intent.getIntExtra("id", 1001)
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, "nexa_reminders") else Notification.Builder(context)
        val notification = builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Nexa reminder")
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, notification)
    }
}
