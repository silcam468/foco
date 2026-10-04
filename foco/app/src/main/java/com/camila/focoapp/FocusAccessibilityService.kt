package com.camila.focoapp

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class FocusAccessibilityService : AccessibilityService() {
    private var lastPkg = ""
    private var lastAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (!Prefs.blocked(this).contains(pkg)) return
        if (!Prefs.blockingNow(this)) return

        val now = SystemClock.elapsedRealtime()
        val repeated = pkg == lastPkg && now - lastAt < 2000L
        // Los cambios de contenido seguidos de la misma app no vuelven a disparar el bloqueo.
        if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && repeated) return

        performGlobalAction(GLOBAL_ACTION_HOME)
        if (!repeated) {
            Prefs.addBlock(this)
            Toast.makeText(this, "Foco: esa app está bloqueada ahora", Toast.LENGTH_SHORT).show()
        }
        lastPkg = pkg
        lastAt = now
    }

    override fun onInterrupt() {}
}
