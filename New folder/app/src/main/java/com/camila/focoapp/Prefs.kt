package com.camila.focoapp

import android.content.Context

object Prefs {
    private const val P = "foco"
    private const val BLOCKED = "blocked"
    private const val ACTIVE = "active"
    private const val END = "end"
    private const val STRICT = "strict"
    private const val START = "start"
    private const val BLOCKS = "blocks"
    private fun p(c: Context) = c.getSharedPreferences(P, Context.MODE_PRIVATE)
    fun blocked(c: Context): MutableSet<String> = p(c).getStringSet(BLOCKED, emptySet())!!.toMutableSet()
    fun setBlocked(c: Context, s: Set<String>) { p(c).edit().putStringSet(BLOCKED, s).apply() }
    fun active(c: Context) = p(c).getBoolean(ACTIVE, false)
    fun setActive(c: Context, v: Boolean) = p(c).edit().putBoolean(ACTIVE, v).apply()
    fun end(c: Context) = p(c).getLong(END, 0L)
    fun setEnd(c: Context, v: Long) = p(c).edit().putLong(END, v).apply()
    fun strict(c: Context) = p(c).getBoolean(STRICT, false)
    fun setStrict(c: Context, v: Boolean) = p(c).edit().putBoolean(STRICT, v).apply()
    fun start(c: Context) = p(c).getLong(START, 0L)
    fun setStart(c: Context, v: Long) = p(c).edit().putLong(START, v).apply()
    fun blocks(c: Context) = p(c).getInt(BLOCKS, 0)
    fun incrementBlocks(c: Context) = p(c).edit().putInt(BLOCKS, blocks(c)+1).apply()
}
