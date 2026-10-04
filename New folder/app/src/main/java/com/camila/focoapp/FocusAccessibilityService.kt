package com.camila.focoapp

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class FocusAccessibilityService: AccessibilityService() {
    private var lastBlocked=""
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if(event==null || event.eventType!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg=event.packageName?.toString() ?: return
        if(pkg==packageName || !Prefs.active(this)) return
        val end=Prefs.end(this)
        if(end>0L && System.currentTimeMillis()>=end){Prefs.setActive(this,false);Prefs.setEnd(this,0);Prefs.setStart(this,0);return}
        if(Prefs.blocked(this).contains(pkg) && pkg!=lastBlocked){
            lastBlocked=pkg; Prefs.incrementBlocks(this)
            performGlobalAction(GLOBAL_ACTION_HOME)
            android.os.Handler(mainLooper).postDelayed({lastBlocked=""},700)
        }
    }
    override fun onInterrupt(){}
}
