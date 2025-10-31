package com.app.emilockerapp.services

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import androidx.activity.result.ActivityResultLauncher

object DeviceAdminManager {

    private lateinit var appContext: Context
    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    /** Call once (e.g., in Application.onCreate or an Activity’s onCreate) */
    fun init(context: Context) {
        appContext = context.applicationContext
        dpm = appContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(appContext, DeviceAdminReceiver::class.java)
    }

    /** Safe accessors */
    fun getDpm(): DevicePolicyManager {
        check(::dpm.isInitialized) { "DeviceAdminManager not initialized. Call init(context) first." }
        return dpm
    }
    fun getAdminComponent(): ComponentName {
        check(::adminComponent.isInitialized) { "DeviceAdminManager not initialized. Call init(context) first." }
        return adminComponent
    }

    /** State */
    fun hasPermission(): Boolean = ::dpm.isInitialized &&
            ::adminComponent.isInitialized &&
            dpm.isAdminActive(adminComponent)

    /** Build the standard enable-admin Intent */
    fun enableIntent(): Intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getAdminComponent())
        putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "This app requires device admin permissions to function as a kiosk."
        )
    }

    /**
     * Triggers the enable-admin sheet using the Activity Result API.
     * Usage:
     *     DeviceAdminManager.requestEnable(this, adminRequestLauncher)
     */
    fun requestEnable(activity: Context, launcher: ActivityResultLauncher<Intent>) {
        if (!::dpm.isInitialized) init(activity)
        if (!hasPermission()) launcher.launch(enableIntent())
    }

    /** Optional helpers you can call anywhere after init() */
    fun removeActiveAdmin() {
        if (hasPermission()) dpm.removeActiveAdmin(getAdminComponent())
    }

    fun lockNow() {
        if (hasPermission()) dpm.lockNow()
    }

    fun isLockTaskPermitted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dpm.isLockTaskPermitted(appContext.packageName)
        } else false

    fun addUserRestriction(restriction: String) {
        if (hasPermission()) dpm.addUserRestriction(getAdminComponent(), restriction)
    }

    fun clearUserRestriction(restriction: String) {
        if (hasPermission()) dpm.clearUserRestriction(getAdminComponent(), restriction)
    }

    /** Convenience wrappers for common restrictions */
    fun blockCoreRestrictions() {
        addUserRestriction(UserManager.DISALLOW_FACTORY_RESET)
        addUserRestriction(UserManager.DISALLOW_SAFE_BOOT)
        addUserRestriction(UserManager.DISALLOW_USB_FILE_TRANSFER)
    }

    fun clearCoreRestrictions() {
        clearUserRestriction(UserManager.DISALLOW_FACTORY_RESET)
        clearUserRestriction(UserManager.DISALLOW_SAFE_BOOT)
        clearUserRestriction(UserManager.DISALLOW_USB_FILE_TRANSFER)
    }
}
