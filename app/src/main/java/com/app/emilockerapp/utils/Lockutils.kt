package com.app.emilockerapp.utils

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Build
import android.widget.Toast
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.app.emilockerapp.services.DeviceAdminReceiver
import com.app.emilockerapp.services.LockCheckerWorker
import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import java.util.concurrent.TimeUnit

// SharedPreferences Constants
private const val BASE_URL = "https://altux.asiradnan.com/api/"
private const val PREF_NAME = "lock_prefs"
private const val KEY_IS_LOCKED = "is_locked"
private const val KEY_SKIP_LOCK_ONCE = "skip_lock_once"

private const val KEY_IS_REGISTERED = "is_registered"
private const val KEY_REG_TOKEN = "reg_token"
private const val KEY_OWNER_NAME = "owner_name"
private const val KEY_OWNER_EMAIL = "owner_email"
private const val KEY_DEVICE_EMI = "device_emi"

/**
 * Returns true if the EMI is currently overdue (i.e. locked state is active).
 */
fun isEMIOverdue(context: Context): Boolean {
    return context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        .getBoolean(KEY_IS_LOCKED, false)
}

/**
 * Persists the EMI lock state in SharedPreferences.
 */
fun setEMILocked(context: Context, locked: Boolean) {
    context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        .edit().putBoolean(KEY_IS_LOCKED, locked).apply()
}

/**
 * Enables a one-time skip for lock screen check.
 * Useful when returning from LockScreenActivity.
 */
fun setSkipLockOnce(context: Context, skip: Boolean) {
    context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        .edit().putBoolean(KEY_SKIP_LOCK_ONCE, skip).apply()
}

/**
 * Returns true if the app should skip locking once (one-time bypass).
 */
fun shouldSkipLock(context: Context): Boolean {
    return context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        .getBoolean(KEY_SKIP_LOCK_ONCE, false)
}

/**
 * Resets the skip-lock-once flag after it's used.
 */
fun clearSkipLock(context: Context) {
    setSkipLockOnce(context, false)
}

/**
 * Schedules a periodic background worker to check EMI status every 15 minutes.
 */
fun scheduleWorker(context: Context) {
    val workRequest = PeriodicWorkRequestBuilder<LockCheckerWorker>(
        15, TimeUnit.MINUTES
    )
        .setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "EMI_LOCK_CHECK",
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
}

fun startKioskMode(activity: Activity) {
    val dpm = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val component = ComponentName(activity, DeviceAdminReceiver::class.java)

    // Whitelist the app if it's the device owner
    if (dpm.isDeviceOwnerApp(activity.packageName)) {
        dpm.setLockTaskPackages(component, arrayOf(activity.packageName))
    }

    // Only start lock task if not already in it and allowed
    if (!activity.isInLockTaskMode()) {
        //activity.startLockTask()
    } else if (!dpm.isLockTaskPermitted(activity.packageName)) {
        Toast.makeText(activity, "Lock task not permitted", Toast.LENGTH_SHORT).show()
    }
}

fun getBaseUrl(): String {
    return BASE_URL
}


fun stopKioskMode(activity: Activity) {
    activity.stopLockTask()
}

fun Activity.isInLockTaskMode(): Boolean {
    val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
}

//Is device registered?  // NEW
fun isRegistered(context: Context): Boolean {
    return context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        .getBoolean(KEY_IS_REGISTERED, false)
}

// Save flag  // NEW
fun setRegistered(context: Context, registered: Boolean) {
    context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        .edit().putBoolean(KEY_IS_REGISTERED, registered).apply()
}

// Save owner info  // NEW
fun saveRegistrationInfo(
    context: Context,
    tokenString: String?,
    ownerName: String?,
    ownerEmail: String?,
    deviceEmi: String?,
) {
    context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        .edit()
        .putString(KEY_REG_TOKEN, tokenString ?: "")
        .putString(KEY_OWNER_NAME, ownerName ?: "")
        .putString(KEY_OWNER_EMAIL, ownerEmail ?: "")
        .putString(KEY_DEVICE_EMI, deviceEmi ?: "")
        .apply()
}

fun getOwnerName(context: Context): String? =
    context.getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString(KEY_OWNER_NAME, null)

fun getOwnerEmail(context: Context): String? =
    context.getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString(KEY_OWNER_EMAIL, null)

fun getSavedToken(context: Context): String? =
    context.getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString(KEY_REG_TOKEN, null)

fun getDeviceImei(context: Context): String? =
    context.getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString(KEY_DEVICE_EMI, "000")

fun getMyRef(context: Context): DatabaseReference{
    return Firebase.database.getReference("appLock").child(getDeviceImei(context)!!).child("locked")

}