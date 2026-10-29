package com.ongar.pengingatabsen

import android.content.Context
import java.util.Calendar
import java.util.Locale

const val EXTRA_TYPE = "type"
const val EXTRA_TEST = "test"

/** Penyimpanan lokal: data absen per tanggal, hari pengingat, dan pengaturan alarm. */
object Store {
    const val TYPE_IN = "in"
    const val TYPE_OUT = "out"

    private const val PREF = "pengingat_absen"
    private const val KEY_DAYS = "days_enabled"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun dateKey(cal: Calendar = Calendar.getInstance()): String =
        String.format(
            Locale.US, "%04d-%02d-%02d",
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
        )

    fun fmt(h: Int, m: Int): String = String.format(Locale.US, "%02d:%02d", h, m)

    // ---------- Data absen ----------
    fun getTime(ctx: Context, type: String, key: String = dateKey()): Long =
        prefs(ctx).getLong("${type}_$key", 0L)

    fun isDone(ctx: Context, type: String, key: String = dateKey()): Boolean =
        getTime(ctx, type, key) > 0L

    fun setTime(ctx: Context, type: String, millis: Long, key: String = dateKey()) {
        prefs(ctx).edit().putLong("${type}_$key", millis).apply()
    }

    fun resetToday(ctx: Context) {
        val k = dateKey()
        prefs(ctx).edit().remove("${TYPE_IN}_$k").remove("${TYPE_OUT}_$k").apply()
    }

    fun resetAll(ctx: Context) {
        val p = prefs(ctx)
        val e = p.edit()
        p.all.keys
            .filter { it.startsWith("${TYPE_IN}_") || it.startsWith("${TYPE_OUT}_") }
            .forEach { e.remove(it) }
        e.apply()
    }

    // ---------- Hari pengingat (default Senin-Jumat) ----------
    private val defaultDays = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY
    )

    private fun days(ctx: Context): Set<Int> {
        val s = prefs(ctx).getStringSet(KEY_DAYS, null) ?: return defaultDays
        return s.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun isDayEnabled(ctx: Context, dow: Int): Boolean = dow in days(ctx)

    fun isTodayEnabled(ctx: Context): Boolean =
        isDayEnabled(ctx, Calendar.getInstance().get(Calendar.DAY_OF_WEEK))

    fun setDayEnabled(ctx: Context, dow: Int, enabled: Boolean) {
        val set = days(ctx).toMutableSet()
        if (enabled) set.add(dow) else set.remove(dow)
        prefs(ctx).edit().putStringSet(KEY_DAYS, set.map { it.toString() }.toSet()).apply()
    }

    // ---------- Jadwal default absen (terpisah dari alarm) ----------
    // Jadwal = jam kerja resmi (default 07:30 masuk, 16:30 pulang). Dipakai untuk tampilan
    // dan untuk kapan notifikasi "belum absen pulang" mulai muncul. Tidak membunyikan alarm.
    fun schedHour(ctx: Context, type: String): Int =
        prefs(ctx).getInt("sched_h_$type", if (type == TYPE_IN) 7 else 16)

    fun schedMinute(ctx: Context, type: String): Int =
        prefs(ctx).getInt("sched_m_$type", 30)

    fun setSchedule(ctx: Context, type: String, h: Int, m: Int) {
        prefs(ctx).edit().putInt("sched_h_$type", h).putInt("sched_m_$type", m).apply()
    }

    fun scheduleMinutes(ctx: Context, type: String): Int =
        schedHour(ctx, type) * 60 + schedMinute(ctx, type)

    fun scheduleLabel(ctx: Context, type: String): String =
        fmt(schedHour(ctx, type), schedMinute(ctx, type))

    // ---------- Alarm: sakelar utama + maksimal 3 waktu per jenis ----------
    const val SLOTS = 3

    fun alarmEnabled(ctx: Context, type: String): Boolean =
        prefs(ctx).getBoolean("alarm_on_$type", true)

    fun setAlarmEnabled(ctx: Context, type: String, on: Boolean) {
        prefs(ctx).edit().putBoolean("alarm_on_$type", on).apply()
    }

    private fun defaultSlotMinutes(type: String, slot: Int): Int {
        val base = if (type == TYPE_IN) 7 * 60 + 30 else 16 * 60 + 30
        return base + slot * 15          // 07:30 / 07:45 / 08:00 dan 16:30 / 16:45 / 17:00
    }

    /** Slot 1 memakai jam alarm lama bila pengguna pernah mengubahnya (migrasi dari versi sebelumnya). */
    fun slotHour(ctx: Context, type: String, slot: Int): Int {
        val legacy = if (slot == 0) prefs(ctx).getInt("alarm_h_$type", -1) else -1
        return prefs(ctx).getInt("slot_h_${type}_$slot", if (legacy >= 0) legacy else defaultSlotMinutes(type, slot) / 60)
    }

    fun slotMinute(ctx: Context, type: String, slot: Int): Int {
        val legacy = if (slot == 0) prefs(ctx).getInt("alarm_m_$type", -1) else -1
        return prefs(ctx).getInt("slot_m_${type}_$slot", if (legacy >= 0) legacy else defaultSlotMinutes(type, slot) % 60)
    }

    fun slotEnabled(ctx: Context, type: String, slot: Int): Boolean =
        prefs(ctx).getBoolean("slot_on_${type}_$slot", slot == 0)

    fun setSlotTime(ctx: Context, type: String, slot: Int, h: Int, m: Int) {
        prefs(ctx).edit().putInt("slot_h_${type}_$slot", h).putInt("slot_m_${type}_$slot", m).apply()
    }

    fun setSlotEnabled(ctx: Context, type: String, slot: Int, on: Boolean) {
        prefs(ctx).edit().putBoolean("slot_on_${type}_$slot", on).apply()
    }

    fun slotLabel(ctx: Context, type: String, slot: Int): String =
        fmt(slotHour(ctx, type, slot), slotMinute(ctx, type, slot))

    /** Jam alarm yang sedang aktif (urut), mis. "07:30, 07:45". Kosong bila tidak ada. */
    fun activeSlotLabels(ctx: Context, type: String): List<String> =
        (0 until SLOTS).filter { slotEnabled(ctx, type, it) }
            .sortedBy { slotHour(ctx, type, it) * 60 + slotMinute(ctx, type, it) }
            .map { slotLabel(ctx, type, it) }

    /** Alarm hanya berbunyi jika: sakelar utama aktif, hari ini hari pengingat, dan belum absen. */
    fun shouldRing(ctx: Context, type: String): Boolean =
        alarmEnabled(ctx, type) && isTodayEnabled(ctx) && !isDone(ctx, type)

    // ---------- Layanan latar belakang ----------
    fun bgServiceEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean("bg_service", true)

    fun setBgServiceEnabled(ctx: Context, on: Boolean) {
        prefs(ctx).edit().putBoolean("bg_service", on).apply()
    }
}
