package com.app.emilockerapp.uilayer.permission

import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext

import android.app.admin.DevicePolicyManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.app.emilockerapp.services.DeviceAdminManager
import com.app.emilockerapp.utils.setDeviceActive

/**
 * Activity wrapper so we can launch it from WelcomeScreen via Intent
 */
class ManagePermissionActivity : ComponentActivity() {

    private val adminRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // You can react to success/failure here if you want
        // result.resultCode == Activity.RESULT_OK means enabled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        setContent { ManagePermissionScreen(adminRequestLauncher) }
    }
}

@Composable
private fun RememberRefreshOnResume(refresh: () -> Unit) {
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) refresh()
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
}

/* =====================================================
   MANAGE PERMISSION SCREEN — independent Composable
   ===================================================== */
// 2) Update your ManagePermissionScreen to include the new button
@Composable
fun ManagePermissionScreen(adminRequestLauncher: ActivityResultLauncher<Intent>) {
    val ctx = LocalContext.current
    val packageName = ctx.packageName
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    var hasDeviceAdmin by remember { mutableStateOf(DeviceAdminManager.hasPermission()) }
    var hasOverlay by remember { mutableStateOf(canDrawOverlays(ctx)) }
    var hasA11y by remember { mutableStateOf(isAccessibilityEnabled(ctx)) }
    var ignoresDoze by remember { mutableStateOf(isIgnoringBatteryOptimizations(ctx)) }
    var iconHidden by remember { mutableStateOf(!isLauncherAliasEnabled(ctx)) }

    // refresh all flags whenever we come back to this screen
    RememberRefreshOnResume {
        hasDeviceAdmin = DeviceAdminManager.hasPermission()
        hasOverlay = canDrawOverlays(ctx)
        hasA11y = isAccessibilityEnabled(ctx)
        ignoresDoze = isIgnoringBatteryOptimizations(ctx)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .systemBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Manage Permissions", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Grant these permissions to keep Emilocker stable.", color = Color.White.copy(alpha = 0.8f))

        PermissionRow(
            "Device Admin Permission",
            hasDeviceAdmin,
            subtitle = "Required to lock or protect your device remotely.",
            onClick = { DeviceAdminManager.requestEnable(ctx, adminRequestLauncher) },
            onAfter = { hasDeviceAdmin = DeviceAdminManager.hasPermission() }
        )

        PermissionRow(
            "Accessibility Permission",
            hasA11y,
            subtitle = "Needed to detect system settings and prevent unauthorized actions.",
            onClick = { openAccessibilitySettings(ctx, launcher) },
            onAfter = { hasA11y = isAccessibilityEnabled(ctx) }
        )

        PermissionRow(
            "App Auto Start Permission",
            false,
            subtitle = "Opens OEM settings (manual)",
            onClick = { openAutoStartSettings(ctx, launcher) }
        )

        PermissionRow(
            "Turn Off Play Protect",
            false,
            subtitle = "Manual step – opens Play Protect screen",
            onClick = { openPlayProtect(ctx, launcher) }
        )

        PermissionRow(
            "Battery Restriction Permission",
            ignoresDoze,
            subtitle = "Allow unrestricted battery usage",
            onClick = { openBatterySettings(ctx, packageName, launcher) },
            onAfter = { ignoresDoze = isIgnoringBatteryOptimizations(ctx) }
        )

        Button(
            onClick = {
                if (!DeviceAdminManager.hasPermission()) {
                    Toast.makeText(ctx, "Please enable device admin permission", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (!isAccessibilityEnabled(ctx)) {
                    Toast.makeText(ctx, "Please enable accessibility permission", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (DeviceAdminManager.hasPermission() && isAccessibilityEnabled(ctx)){
                    Toast.makeText(ctx, "Device Already Active", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                Toast.makeText(ctx, "Device Activate Successfully", Toast.LENGTH_SHORT).show()
                setDeviceActive(ctx, true)
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3), // blue background
                contentColor = Color.White
            )
        ) {
            Text("Active Device", fontWeight = FontWeight.SemiBold)
        }

    }
}

/* =====================================================
   Reusable Permission Row Component
   ===================================================== */
@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    subtitle: String? = null,
    onClick: () -> Unit,
    onAfter: (() -> Unit)? = null
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                subtitle?.let {
                    Text(it, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
            if (granted) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
            } else {
                FilledTonalButton(onClick = { onClick(); onAfter?.invoke() }) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Action")
                }
            }
        }
    }
}

private fun requestDeviceAdmin(
    ctx: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    try {
        val cn = ComponentName(ctx, Class.forName("com.app.emilockerapp.services.MyDeviceAdminReceiver"))
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cn)
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Grant admin to enable lock control.")
        launcher.launch(intent)
    } catch (_: Exception) {
        launcher.launch(Intent(Settings.ACTION_SECURITY_SETTINGS))
    }
}

private fun isAccessibilityEnabled(ctx: Context): Boolean {
    val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val target = ComponentName(ctx, Class.forName("com.app.emilockerapp.services.SettingsWatchService")).flattenToString()
    return enabled.split(':').any { it.equals(target, ignoreCase = true) }
}

private fun openAccessibilitySettings(ctx: Context, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    launcher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
}

private fun canDrawOverlays(ctx: Context) = Settings.canDrawOverlays(ctx)
private fun openOverlaySettings(ctx: Context, pkg: String, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    launcher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$pkg")))
}

private fun isIgnoringBatteryOptimizations(ctx: Context): Boolean {
    val pm = ctx.getSystemService<PowerManager>() ?: return false
    return pm.isIgnoringBatteryOptimizations(ctx.packageName)
}

private fun requestIgnoreBatteryOptimizations(ctx: Context, pkg: String, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    launcher.launch(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:$pkg")))
}

private fun openBatterySettings(ctx: Context, pkg: String, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val intent =
        if (Build.VERSION.SDK_INT >= 31) Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).setData(Uri.parse("package:$pkg"))
        else Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    launcher.launch(intent)
}

private fun openPlayProtect(ctx: Context, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val intents = listOf(
        Intent("com.google.android.gms.security.settings.ACTION_SECURITY_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        Intent("com.google.android.gms.app.settings.SECURITY_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    for (i in intents) try { launcher.launch(i); return } catch (_: Exception) {}
    launcher.launch(Intent(Settings.ACTION_SECURITY_SETTINGS))
}

private fun openAutoStartSettings(ctx: Context, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val intents = listOf(
        Intent().setComponent(ComponentName("com.miui.securitycenter","com.miui.permcenter.autostart.AutoStartManagementActivity")),
        Intent().setComponent(ComponentName("com.coloros.safecenter","com.coloros.safecenter.startupapp.StartupAppListActivity")),
        Intent().setComponent(ComponentName("com.iqoo.secure","com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
        Intent().setComponent(ComponentName("com.oneplus.security","com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"))
    )
    for (i in intents) try { launcher.launch(i); return } catch (_: Exception) {}
    launcher.launch(Intent(Settings.ACTION_SETTINGS))
}

/* ---------- App Icon Hide ---------- */
private const val LAUNCHER_ALIAS_COMPONENT = "com.app.emilockerapp.LauncherAlias"

private fun isLauncherAliasEnabled(ctx: Context): Boolean {
    return try {
        val pm = ctx.packageManager
        val cn = ComponentName(ctx.packageName, LAUNCHER_ALIAS_COMPONENT)
        pm.getComponentEnabledSetting(cn) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    } catch (_: Exception) { true }
}

private fun setLauncherAliasEnabled(ctx: Context, enable: Boolean) {
    try {
        val pm = ctx.packageManager
        val cn = ComponentName(ctx.packageName, LAUNCHER_ALIAS_COMPONENT)
        pm.setComponentEnabledSetting(
            cn,
            if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    } catch (_: Exception) { }
}
