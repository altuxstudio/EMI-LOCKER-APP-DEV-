package com.app.emilockerapp.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.app.emilockerapp.uilayer.views.MainActivity
import com.app.emilockerapp.utils.getMyRef

class SettingsWatchService : AccessibilityService() {

    companion object {
        private const val TAG = "SettingsWatch"

        // Core Settings app
        private const val SETTINGS_PKG = "com.android.settings"

        // AOSP (historical & current) class names you might see in the reset flow.
        // Not exhaustive; log unknown classes you observe on target devices.
        private val POSSIBLE_RESET_CLASSES = setOf(
            // AOSP/GMS variants seen across releases
            "com.android.settings.MasterClear",
            "com.android.settings.MasterClearConfirm",
            "com.android.settings.Settings\$MasterClearActivity",
            "com.android.settings.Settings\$MasterClearConfirmActivity",
            "com.android.settings.factoryreset.FactoryResetActivity",
            "com.android.settings.SubSettings"
        )

        // Phrases that commonly appear on UI nodes (localized later).
        // Add your target languages/brands here.
        private val RESET_KEYWORDS = listOf(
            // English
            "Factory data reset", "Erase all data", "Reset options", "Delete all data",
            // Bengali (Bangla)
            "ফ্যাক্টরি ডাটা রিসেট", "সমস্ত ডেটা মুছুন", "রিসেট অপশন",
            // Generic
            "factory reset", "master clear"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString().orEmpty()
        if (pkg != SETTINGS_PKG) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val cls = event.className?.toString().orEmpty()
                if (cls in POSSIBLE_RESET_CLASSES || cls.contains("MasterClear", true)) {
                    Log.i(TAG, "Entered possible Factory Reset screen: $cls")
                    notifyEnteredResetFlow(cls)
                }
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // User tapped something inside Settings — check if it looks like a reset action.
                if (looksLikeFactoryResetClick(event)) {
                    Log.i(TAG, "User tapped a Factory Reset related control.")
                    onFactoryResetTapped()
                }
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // As user navigates deeper, scan the node tree for reset keywords.
                rootInActiveWindow?.let { root ->
                    if (treeContainsResetUI(root)) {
                        Log.d(TAG, "Reset UI visible in current window.")
                        // Optional: notify your app UI to react (e.g., warn, log, etc.)
                    }
                }
            }
        }
    }

    private fun looksLikeFactoryResetClick(event: AccessibilityEvent): Boolean {
        val texts = (event.text ?: emptyList()).joinToString(" ").lowercase()
        val desc  = event.contentDescription?.toString()?.lowercase().orEmpty()
        val anyHit = (RESET_KEYWORDS.any { kw -> texts.contains(kw.lowercase()) || desc.contains(kw.lowercase()) })
        if (anyHit) return true

        // Fall back: inspect the clicked node & its parents for known resource-ids or text.
        val node = event.source ?: return false
        return nodeOrParentsIndicateReset(node)
    }

    private fun nodeOrParentsIndicateReset(node: AccessibilityNodeInfo?): Boolean {
        var cur = node
        repeat(6) { // climb a few levels
            if (cur == null) return false
            val txt = (cur.text?.toString() ?: "") + " " + (cur.contentDescription?.toString() ?: "")
            val hit = RESET_KEYWORDS.any { kw -> txt.contains(kw, ignoreCase = true) }
            val idHit = cur.viewIdResourceName?.contains("reset", ignoreCase = true) == true ||
                    cur.viewIdResourceName?.contains("erase", ignoreCase = true) == true
            if (hit || idHit) return true
            cur = cur.parent
        }
        return false
    }

    private fun treeContainsResetUI(root: AccessibilityNodeInfo): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo?>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val n = queue.removeFirst() ?: continue
            val txt = (n.text?.toString() ?: "") + " " + (n.contentDescription?.toString() ?: "")
            val id  = n.viewIdResourceName ?: ""
            if (RESET_KEYWORDS.any { kw -> txt.contains(kw, true) } ||
                id.contains("master_clear", true) ||
                id.contains("factory_reset", true) ||
                id.contains("erase", true)) {
                return true
            }
            for (i in 0 until n.childCount) queue.add(n.getChild(i))
        }
        return false
    }

    private fun notifyEnteredResetFlow(className: String) {
        sendBroadcast(Intent("com.example.app.RESET_FLOW_ENTERED").putExtra("cls", className))
        bringAppToFront()
    }

    private fun onFactoryResetTapped() {
        // Your app reaction here (e.g., show warning UI, log analytics, etc.)
        // Example: local broadcast or bound-service callback to app process
        sendBroadcast(Intent("com.example.app.FACTORY_RESET_TAPPED"))
        bringAppToFront()

        getMyRef(this).setValue(true)
    }

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
