package com.camila.focoapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var timer: TextView
    private lateinit var focusButton: Button
    private lateinit var strictSwitch: Switch
    private lateinit var appsContainer: LinearLayout
    private lateinit var search: EditText
    private var minutes = 45
    private var allApps = listOf<AppItem>()
    private data class AppItem(val label: String, val pkg: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status=findViewById(R.id.status); timer=findViewById(R.id.timer); focusButton=findViewById(R.id.focusButton)
        appsContainer=findViewById(R.id.appsContainer); search=findViewById(R.id.search); strictSwitch=findViewById(R.id.strictSwitch)
        findViewById<Button>(R.id.accessibilityButton).setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        findViewById<Button>(R.id.timeButton).setOnClickListener { chooseTime() }
        focusButton.setOnClickListener { toggleFocus() }
        strictSwitch.isChecked=Prefs.strict(this)
        strictSwitch.setOnCheckedChangeListener { _, checked -> Prefs.setStrict(this, checked); refresh() }
        search.addTextChangedListener(object: TextWatcher { override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}; override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){renderApps(s?.toString().orEmpty())}; override fun afterTextChanged(s:Editable?){}})
        loadApps(); refresh()
    }
    private fun loadApps(){
        val pm=packageManager; val mine=packageName
        allApps=pm.getInstalledApplications(0).filter { it.packageName!=mine && (it.flags and ApplicationInfo.FLAG_SYSTEM)==0 }
            .map { AppItem(pm.getApplicationLabel(it).toString(),it.packageName) }.sortedBy{it.label.lowercase(Locale.getDefault())}
        renderApps("")
    }
    private fun renderApps(query:String){
        appsContainer.removeAllViews(); val blocked=Prefs.blocked(this); val q=query.trim().lowercase(Locale.getDefault())
        allApps.filter{q.isEmpty() || it.label.lowercase(Locale.getDefault()).contains(q)}.forEach { app ->
            val row=CheckBox(this); row.text=app.label; row.textSize=16f; row.isChecked=blocked.contains(app.pkg); row.tag=app.pkg
            row.setPadding(4,10,4,10)
            row.setOnCheckedChangeListener { _,checked -> val s=Prefs.blocked(this); if(checked)s.add(app.pkg)else s.remove(app.pkg); Prefs.setBlocked(this,s); refresh() }
            appsContainer.addView(row,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        findViewById<TextView>(R.id.selectedCount).text="${blocked.size} apps seleccionadas"
    }
    private fun chooseTime(){
        val labels=arrayOf("15 minutos","30 minutos","45 minutos","60 minutos","90 minutos","2 horas","Sin límite")
        val vals=intArrayOf(15,30,45,60,90,120,0)
        val current=vals.indexOf(minutes).coerceAtLeast(0)
        AlertDialog.Builder(this).setTitle("Duración de concentración").setSingleChoiceItems(labels,current){d,w->minutes=vals[w]; findViewById<Button>(R.id.timeButton).text="Duración: ${labels[w]}"; d.dismiss()}.show()
    }
    private fun toggleFocus(){
        if(Prefs.active(this)){
            stopFocus()
            Toast.makeText(this,"Concentración desactivada.",Toast.LENGTH_SHORT).show()
        } else {
            if(Prefs.blocked(this).isEmpty()){Toast.makeText(this,"Elegí al menos una app para bloquear.",Toast.LENGTH_SHORT).show();return}
            val end=if(minutes==0)0 else System.currentTimeMillis()+minutes*60000L
            Prefs.setStart(this,System.currentTimeMillis()); Prefs.setEnd(this,end); Prefs.setActive(this,true); refresh()
        }
    }
    private fun stopFocus(){Prefs.setActive(this,false);Prefs.setEnd(this,0);Prefs.setStart(this,0);refresh()}
    private fun refresh(){
        val active=Prefs.active(this); val strict=Prefs.strict(this)
        status.text=if(active) "● CONCENTRACIÓN ACTIVA" else "○ Concentración desactivada"
        focusButton.text=if(active) "Desactivar concentración" else "Empezar a concentrarme"
        focusButton.isEnabled=true
        timer.text=if(active){val e=Prefs.end(this);if(e==0L)"Sin límite de tiempo" else "Termina a las ${SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date(e))}"}else "Listo para empezar"
        findViewById<TextView>(R.id.selectedCount).text="${Prefs.blocked(this).size} apps seleccionadas"
    }
    override fun onResume(){super.onResume(); if(::status.isInitialized) refresh()}
}
