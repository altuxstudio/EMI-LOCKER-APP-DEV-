package com.app.emilockerapp.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.app.emilockerapp.R
import com.app.emilockerapp.uilayer.views.MainActivity
import com.app.emilockerapp.utils.getDeviceImei
import com.app.emilockerapp.utils.getMyRef
import com.google.firebase.database.*

/**
 * Foreground service that:
 * 1. Listens to Firebase Realtime DB for lock/unlock changes.
 * 2. Brings MainActivity to front when lock is triggered.
 * 3. Persists across app removal and device reboot.
 */
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
        // Ensures the service restarts if killed by the system
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Start listening to Firebase for real-time lock updates
     */
    private fun startFirebaseListener() {
        firebaseListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(Boolean::class.java) ?: false
                if (value) {
                    bringAppToFront()   // Open MainActivity
                    //enableKioskMode()

                } else {
                    //bringAppToFront()
                    //disableKioskMode()
                }

                //Toast.makeText(this@LockService, "service: $value", Toast.LENGTH_SHORT).show()

            }

            override fun onCancelled(error: DatabaseError) {
                // Handle DB error if needed
            }
        }
        getMyRef(this).addValueEventListener(firebaseListener!!)
    }

    /**
     * Bring MainActivity to front, even if the app was removed from Recents
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

    /**
     * Apply your lock actions here (e.g., DevicePolicyManager restrictions, kiosk mode)
     */
    private fun applyLock() {
        // TODO: Implement actual lock logic
    }

    /**
     * Remove restrictions when unlocked
     */
    private fun releaseLock() {
        // TODO: Implement unlock logic
    }

    private fun enableKioskMode() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("ENABLE_KIOSK", true)
        }
        startActivity(intent)
    }

    private fun disableKioskMode() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("DISABLE_KIOSK", true)
        }
        startActivity(intent)
    }

    /**
     * Foreground notification to keep service alive
     */
    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.emi_lock_text))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    /**
     * Create a notification channel for Android O+
     */
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
