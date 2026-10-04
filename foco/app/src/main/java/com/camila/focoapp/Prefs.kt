package com.camila.focoapp

import android.content.Context
import java.util.Calendar

/** Un horario automático. days: bit 0 = lunes ... bit 6 = domingo. */
data class Schedule(val id: Long, val days: Int, val startMin: Int, val endMin: Int, val enabled: Boolean) {
    fun hasDay(i: Int): Boolean = ((days shr i) and 1) == 1

    fun isActiveAt(cal: Calendar): Boolean {
        if (!enabled || days == 0 || startMin == endMin) return false
        val today = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val yesterday = (today + 6) % 7
        val m = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return if (startMin < endMin) {
            hasDay(today) && m >= startMin && m < endMin
        } else {
            // cruza la medianoche (ej. 22:00 a 07:00)
            (hasDay(today) && m >= startMin) || (hasDay(yesterday) && m < endMin)
        }
    }
}

object Prefs {
    private fun p(c: Context) = c.getSharedPreferences("foco", Context.MODE_PRIVATE)

    // ---- apps bloqueadas
    fun blocked(c: Context): MutableSet<String> =
        p(c).getStringSet("blocked", emptySet<String>())!!.toMutableSet()

    fun setBlocked(c: Context, s: Set<String>) {
        p(c).edit().putStringSet("blocked", HashSet(s)).apply()
    }

    // ---- sesión manual
    fun minutes(c: Context): Int = p(c).getInt("minutes", 45)
    fun setMinutes(c: Context, v: Int) { p(c).edit().putInt("minutes", v).apply() }
    fun end(c: Context): Long = p(c).getLong("end", 0L)

    fun startManual(c: Context, minutes: Int) {
        val end = if (minutes == 0) 0L else System.currentTimeMillis() + minutes * 60000L
        p(c).edit().putBoolean("active", true).putLong("end", end).apply()
    }

    fun stopManual(c: Context) {
        p(c).edit().putBoolean("active", false).putLong("end", 0L).apply()
    }

    /** True si hay una sesión manual en curso. Si ya se pasó el tiempo, la cierra. */
    fun manualActive(c: Context): Boolean {
        if (!p(c).getBoolean("active", false)) return false
        val e = end(c)
        if (e > 0L && System.currentTimeMillis() >= e) {
            stopManual(c)
            return false
        }
        return true
    }

    // ---- modo estricto
    fun strict(c: Context): Boolean = p(c).getBoolean("strict", false)
    fun setStrict(c: Context, v: Boolean) { p(c).edit().putBoolean("strict", v).apply() }

    // ---- horarios
    fun schedules(c: Context): List<Schedule> {
        val raw = p(c).getString("schedules", "") ?: ""
        return raw.split(";").mapNotNull { line ->
            val f = line.split(",")
            if (f.size != 5) null
            else try {
                Schedule(f[0].toLong(), f[1].toInt(), f[2].toInt(), f[3].toInt(), f[4] == "1")
            } catch (e: NumberFormatException) {
                null
            }
        }
    }

    fun saveSchedules(c: Context, list: List<Schedule>) {
        val raw = list.joinToString(";") {
            "${it.id},${it.days},${it.startMin},${it.endMin},${if (it.enabled) 1 else 0}"
        }
        p(c).edit().putString("schedules", raw).apply()
    }

    fun activeSchedule(c: Context): Schedule? {
        val cal = Calendar.getInstance()
        return schedules(c).firstOrNull { it.isActiveAt(cal) }
    }

    // ---- estado general
    fun blockingNow(c: Context): Boolean = manualActive(c) || activeSchedule(c) != null

    /** Con modo estricto no se puede cambiar nada mientras haya un final definido. */
    fun locked(c: Context): Boolean {
        if (!strict(c)) return false
        val timedManual = manualActive(c) && end(c) > 0L
        return timedManual || activeSchedule(c) != null
    }

    // ---- estadística
    fun blocks(c: Context): Int = p(c).getInt("blocks", 0)
    fun addBlock(c: Context) { p(c).edit().putInt("blocks", blocks(c) + 1).apply() }
}
