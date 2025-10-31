package com.app.emilockerapp.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.app.emilockerapp.R
import com.app.emilockerapp.uilayer.views.MainActivity
import com.app.emilockerapp.utils.getMyRef
import com.google.firebase.database.*

class LockService : Service() {

    private val channelId = "lock_channel"
    private val notificationId = 101
    private var firebaseListener: ValueEventListener? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(notificationId, buildNotification(getString(R.string.emi_app_running)))
        startFirebaseListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // restart automatically if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** ✅ Keep service alive even after app removed from recents */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartServiceIntent = Intent(applicationContext, LockService::class.java).apply {
            `package` = packageName
        }

        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmService = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        )
    }

    /**
     * Start listening to Firebase for real-time lock updates
     */
    private fun startFirebaseListener() {
        firebaseListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(Boolean::class.java) ?: false
                if (value) {
                    bringAppToFront()
                    Toast.makeText(this@LockService, "Device Locked", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@LockService, "Device Unlocked", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        getMyRef(this).addValueEventListener(firebaseListener!!)
    }

    /**
     * Bring MainActivity to front even if app was killed
     */
    private fun bringAppToFront() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        startActivity(launchIntent)
    }

    /** 🔔 Foreground notification to keep alive */
    /*private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.emi_lock_text))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }*/

    private fun buildNotification(text: String): Notification {
        // Intent that launches MainActivity
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.emi_lock_text))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            // 👇 this is key: tells Android we want to show the Activity as a full-screen UI
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Device Lock Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
