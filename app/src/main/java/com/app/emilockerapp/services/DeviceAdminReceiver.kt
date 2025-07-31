package com.app.emilockerapp.services

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import android.util.Log
import android.widget.Toast
import android.provider.Settings
import androidx.annotation.RequiresApi
import android.app.admin.DevicePolicyManager


class DeviceAdminReceiver : DeviceAdminReceiver() {

    class AdminPolicyManager(private val context: Context) {
        private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        private val admin = ComponentName(context, DeviceAdminReceiver::class.java)

        @RequiresApi(Build.VERSION_CODES.S)
        fun blockUsbDebuggingAndAccess() {

            if (dpm.isDeviceOwnerApp(context.packageName)) {
                // Disable USB file transfer (default USB mode)
                dpm.setUsbDataSignalingEnabled(false)

                // Optional (disable screen unlock via USB debugging)
                dpm.addUserRestriction(admin, UserManager.DISALLOW_USB_FILE_TRANSFER)

                // Block ADB debugging
                dpm.setGlobalSetting(admin, Settings.Global.ADB_ENABLED, "0")
            } else {
                Log.e("Policy", "Not device owner")
            }
        }
    }


    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Device Admin Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Device Admin Disabled", Toast.LENGTH_SHORT).show()
    }

    //val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    //val admin = getWho(context)

    // 🔐  Block USB data + ADB
    //dpm.addUserRestriction(admin, UserManager.DISALLOW_USB_FILE_TRANSFER)
    //dpm.addUserRestriction(admin, UserManager.DISALLOW_DEBUGGING_FEATURES)

    // Optional: toast for confirmation
    //Toast.makeText(context, "USB & ADB blocked", Toast.LENGTH_SHORT).show()
}


