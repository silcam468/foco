package com.camila.focoapp

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

// Apps que nunca se ofrecen para bloquear (llamadas / emergencias).
private val SAFE_PKGS = listOf(
    "com.android.dialer", "com.google.android.dialer", "com.samsung.android.dialer",
    "com.samsung.android.incallui", "com.android.server.telecom", "com.android.emergency"
)

private val DURATION_LABELS = arrayOf(
    "15 minutos", "30 minutos", "45 minutos", "1 hora", "1 h 30 min",
    "2 horas", "3 horas", "4 horas", "Sin límite"
)
private val DURATION_VALUES = intArrayOf(15, 30, 45, 60, 90, 120, 180, 240, 0)

class MainActivity : Activity() {

    private data class AppItem(val label: String, val pkg: String)

    private lateinit var status: TextView
    private lateinit var timer: TextView
    private lateinit var focusButton: Button
    private lateinit var timeButton: Button
    private lateinit var strictSwitch: Switch
    private lateinit var permCard: LinearLayout
    private lateinit var schedulesBox: LinearLayout
    private lateinit var appsBox: LinearLayout
    private lateinit var selectedCount: TextView
    private lateinit var blocksCount: TextView
    private lateinit var search: EditText

    private var allApps = listOf<AppItem>()
    private var renderedLocked: Boolean? = null
    private val ui = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            refresh()
            ui.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        timer = findViewById(R.id.timer)
        focusButton = findViewById(R.id.focusButton)
        timeButton = findViewById(R.id.timeButton)
        strictSwitch = findViewById(R.id.strictSwitch)
        permCard = findViewById(R.id.permCard)
        schedulesBox = findViewById(R.id.schedulesBox)
        appsBox = findViewById(R.id.appsBox)
        selectedCount = findViewById(R.id.selectedCount)
        blocksCount = findViewById(R.id.blocksCount)
        search = findViewById(R.id.search)
        val accessibilityButton = findViewById<Button>(R.id.accessibilityButton)
        val addScheduleButton = findViewById<Button>(R.id.addScheduleButton)

        styleButton(focusButton, true)
        styleButton(timeButton, false)
        styleButton(accessibilityButton, true)
        styleButton(addScheduleButton, false)

        accessibilityButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        focusButton.setOnClickListener { toggleFocus() }
        timeButton.setOnClickListener { chooseTime() }
        addScheduleButton.setOnClickListener {
            if (Prefs.locked(this)) toast("Modo estricto: no podés cambiar esto ahora.")
            else editSchedule(null)
        }
        strictSwitch.setOnClickListener {
            if (Prefs.locked(this)) {
                strictSwitch.isChecked = Prefs.strict(this)
                toast("Modo estricto: no podés cambiarlo mientras la concentración está activa.")
            } else {
                Prefs.setStrict(this, strictSwitch.isChecked)
            }
            refresh()
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                renderApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadApps()
        renderSchedules()
        refresh()
    }

    override fun onResume() {
        super.onResume()
        ui.removeCallbacks(tick)
        ui.post(tick)
    }

    override fun onPause() {
        super.onPause()
        ui.removeCallbacks(tick)
    }

    // ---------------------------------------------------------------- utilidades

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun toast(t: String) {
        Toast.makeText(this, t, Toast.LENGTH_LONG).show()
    }

    private fun styleButton(b: Button, primary: Boolean) {
        b.setBackgroundResource(if (primary) R.drawable.btn_primary else R.drawable.btn_secondary)
        b.setTextColor(if (primary) Color.WHITE else 0xFF381E72.toInt())
    }

    private fun fmt(min: Int): String = String.format(Locale.US, "%02d:%02d", min / 60, min % 60)

    private fun daysLabel(d: Int): String {
        if (d == 127) return "Todos los días"
        if (d == 31) return "Lunes a viernes"
        if (d == 96) return "Fines de semana"
        val n = arrayOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        return (0 until 7).filter { ((d shr it) and 1) == 1 }.joinToString(", ") { n[it] }
    }

    private fun durationLabel(min: Int): String {
        val i = DURATION_VALUES.indexOf(min)
        return if (i >= 0) DURATION_LABELS[i] else "$min min"
    }

    private fun serviceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val cn = ComponentName(this, FocusAccessibilityService::class.java)
        return enabled.split(':').any {
            it.equals(cn.flattenToString(), true) || it.equals(cn.flattenToShortString(), true)
        }
    }

    // ---------------------------------------------------------------- estado

    private fun refresh() {
        val manual = Prefs.manualActive(this)
        val sched = Prefs.activeSchedule(this)
        val locked = Prefs.locked(this)

        permCard.visibility = if (serviceEnabled()) View.GONE else View.VISIBLE

        if (manual) {
            status.text = "● CONCENTRACIÓN ACTIVA"
            val e = Prefs.end(this)
            timer.text = if (e > 0L) remaining(e - System.currentTimeMillis()) else "Sin límite de tiempo"
            focusButton.text = "Terminar concentración"
        } else if (sched != null) {
            status.text = "● ACTIVA POR HORARIO AUTOMÁTICO"
            timer.text = "Hasta las ${fmt(sched.endMin)}"
            focusButton.text = "Empezar una sesión ahora"
        } else {
            status.text = "○ Concentración desactivada"
            timer.text = "Lista para empezar"
            focusButton.text = "Empezar a concentrarme"
        }
        timeButton.text = "Duración: ${durationLabel(Prefs.minutes(this))}"
        strictSwitch.isChecked = Prefs.strict(this)
        selectedCount.text = "${Prefs.blocked(this).size} seleccionadas"
        blocksCount.text = "Veces que Foco te frenó: ${Prefs.blocks(this)}"

        if (renderedLocked != locked) {
            renderedLocked = locked
            renderApps(search.text.toString())
            renderSchedules()
        }
    }

    private fun remaining(ms: Long): String {
        val s = if (ms < 0L) 0L else ms / 1000L
        val h = s / 3600L
        val m = (s % 3600L) / 60L
        val sec = s % 60L
        return if (h > 0L) String.format(Locale.US, "Quedan %d:%02d:%02d", h, m, sec)
        else String.format(Locale.US, "Quedan %02d:%02d", m, sec)
    }

    private fun toggleFocus() {
        if (Prefs.manualActive(this)) {
            if (Prefs.locked(this)) {
                toast("Modo estricto: no podés terminar antes de que se acabe el tiempo.")
                return
            }
            Prefs.stopManual(this)
            toast("Concentración terminada.")
        } else {
            if (!serviceEnabled()) {
                toast("Primero activá el permiso de Accesibilidad (aviso de arriba).")
                return
            }
            if (Prefs.blocked(this).isEmpty()) {
                toast("Elegí al menos una app para bloquear.")
                return
            }
            Prefs.startManual(this, Prefs.minutes(this))
        }
        refresh()
    }

    private fun chooseTime() {
        val current = DURATION_VALUES.indexOf(Prefs.minutes(this)).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Duración de la concentración")
            .setSingleChoiceItems(DURATION_LABELS, current) { d, which ->
                Prefs.setMinutes(this, DURATION_VALUES[which])
                d.dismiss()
                refresh()
            }
            .show()
    }

    // ---------------------------------------------------------------- apps

    private fun loadApps() {
        val pm = packageManager
        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

        val skip = HashSet<String>(SAFE_PKGS)
        skip.add(packageName)
        for (r in pm.queryIntentActivities(home, 0)) skip.add(r.activityInfo.packageName)

        val seen = HashSet<String>()
        val list = ArrayList<AppItem>()
        for (r in pm.queryIntentActivities(launch, 0)) {
            val pkg = r.activityInfo.packageName
            if (pkg in skip || !seen.add(pkg)) continue
            list.add(AppItem(r.loadLabel(pm).toString(), pkg))
        }
        allApps = list.sortedBy { it.label.lowercase(Locale.getDefault()) }
        renderApps(search.text.toString())
    }

    private fun renderApps(query: String) {
        appsBox.removeAllViews()
        val blocked = Prefs.blocked(this)
        val locked = Prefs.locked(this)
        val q = query.trim().lowercase(Locale.getDefault())

        for (app in allApps) {
            if (q.isNotEmpty() && !app.label.lowercase(Locale.getDefault()).contains(q)) continue
            val row = CheckBox(this)
            row.text = app.label
            row.textSize = 16f
            row.isChecked = blocked.contains(app.pkg)
            row.isEnabled = !locked
            row.setPadding(dp(4), dp(10), dp(4), dp(10))
            row.setOnCheckedChangeListener { _, checked ->
                val s = Prefs.blocked(this)
                if (checked) s.add(app.pkg) else s.remove(app.pkg)
                Prefs.setBlocked(this, s)
                selectedCount.text = "${s.size} seleccionadas"
            }
            appsBox.addView(
                row,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
        }
        if (allApps.isEmpty()) {
            val empty = TextView(this)
            empty.text = "No encontré apps instaladas."
            appsBox.addView(empty)
        }
    }

    // ---------------------------------------------------------------- horarios

    private fun renderSchedules() {
        schedulesBox.removeAllViews()
        val list = Prefs.schedules(this)
        val locked = Prefs.locked(this)

        if (list.isEmpty()) {
            val empty = TextView(this)
            empty.text = "Todavía no tenés horarios."
            empty.textSize = 14f
            empty.setTextColor(0xFF777777.toInt())
            empty.setPadding(0, dp(8), 0, dp(8))
            schedulesBox.addView(empty)
            return
        }

        for (s in list) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(dp(14), dp(10), dp(14), dp(10))
            row.setBackgroundResource(R.drawable.card)

            val txt = TextView(this)
            txt.text = "${daysLabel(s.days)}\n${fmt(s.startMin)} – ${fmt(s.endMin)}"
            txt.textSize = 15f
            txt.setTextColor(0xFF222222.toInt())
            row.addView(txt, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            val sw = Switch(this)
            sw.isChecked = s.enabled
            sw.isEnabled = !locked
            sw.setOnCheckedChangeListener { _, on ->
                val updated = Prefs.schedules(this).map { if (it.id == s.id) it.copy(enabled = on) else it }
                Prefs.saveSchedules(this, updated)
                refresh()
            }
            row.addView(sw)

            row.setOnClickListener {
                if (Prefs.locked(this)) toast("Modo estricto: no podés cambiar esto ahora.")
                else editSchedule(s)
            }

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, dp(4), 0, dp(4))
            schedulesBox.addView(row, lp)
        }
    }

    private fun editSchedule(existing: Schedule?) {
        var days = existing?.days ?: 31
        var start = existing?.startMin ?: (9 * 60)
        var end = existing?.endMin ?: (13 * 60)

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(20), dp(12), dp(20), 0)

        val names = arrayOf("L", "M", "X", "J", "V", "S", "D")
        val chipsRow = LinearLayout(this)
        chipsRow.orientation = LinearLayout.HORIZONTAL
        for (i in 0 until 7) {
            val chip = TextView(this)
            chip.text = names[i]
            chip.gravity = Gravity.CENTER
            chip.textSize = 15f
            fun paint() {
                val on = ((days shr i) and 1) == 1
                chip.setBackgroundResource(if (on) R.drawable.chip_on else R.drawable.chip_off)
                chip.setTextColor(if (on) Color.WHITE else 0xFF444444.toInt())
            }
            paint()
            chip.setOnClickListener {
                days = days xor (1 shl i)
                paint()
            }
            val lp = LinearLayout.LayoutParams(0, dp(40), 1f)
            lp.setMargins(dp(2), 0, dp(2), 0)
            chipsRow.addView(chip, lp)
        }
        box.addView(chipsRow)

        val startBtn = Button(this)
        val endBtn = Button(this)
        styleButton(startBtn, false)
        styleButton(endBtn, false)
        fun labels() {
            startBtn.text = "Desde: ${fmt(start)}"
            endBtn.text = "Hasta: ${fmt(end)}"
        }
        labels()
        startBtn.setOnClickListener {
            TimePickerDialog(this, { _, h, m -> start = h * 60 + m; labels() }, start / 60, start % 60, true).show()
        }
        endBtn.setOnClickListener {
            TimePickerDialog(this, { _, h, m -> end = h * 60 + m; labels() }, end / 60, end % 60, true).show()
        }
        val btnLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))
        btnLp.setMargins(0, dp(12), 0, 0)
        box.addView(startBtn, btnLp)
        box.addView(endBtn, btnLp)

        val hint = TextView(this)
        hint.text = "Si la hora final es menor que la inicial, el horario cruza la medianoche (ej. 22:00 a 07:00)."
        hint.textSize = 12f
        hint.setTextColor(0xFF777777.toInt())
        hint.setPadding(0, dp(10), 0, dp(4))
        box.addView(hint)

        val builder = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Nuevo horario" else "Editar horario")
            .setView(box)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
        if (existing != null) builder.setNeutralButton("Eliminar", null)
        val dialog = builder.create()
        dialog.show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            if (days == 0 || start == end) {
                toast("Elegí al menos un día y que la hora de inicio y la de fin sean distintas.")
                return@setOnClickListener
            }
            val list = Prefs.schedules(this).toMutableList()
            if (existing == null) {
                list.add(Schedule(System.currentTimeMillis(), days, start, end, true))
            } else {
                val idx = list.indexOfFirst { it.id == existing.id }
                if (idx >= 0) list[idx] = Schedule(existing.id, days, start, end, existing.enabled)
            }
            Prefs.saveSchedules(this, list)
            dialog.dismiss()
            renderSchedules()
            refresh()
        }
        if (existing != null) {
            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
                Prefs.saveSchedules(this, Prefs.schedules(this).filter { it.id != existing.id })
                dialog.dismiss()
                renderSchedules()
                refresh()
            }
        }
    }
}
