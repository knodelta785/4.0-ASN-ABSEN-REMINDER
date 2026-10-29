package com.ongar.pengingatabsen

import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ongar.pengingatabsen.databinding.ActivityAlarmSettingBinding
import com.ongar.pengingatabsen.databinding.ItemAlarmSlotBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Menu Alarm Absen Masuk / Pulang: sakelar utama + maksimal 3 waktu alarm (dibedakan oleh EXTRA_TYPE). */
class AlarmSettingActivity : AppCompatActivity() {
    private lateinit var b: ActivityAlarmSettingBinding
    private lateinit var type: String
    private var updating = false
    private val slotViews = mutableListOf<ItemAlarmSlotBinding>()

    private val nextFmt = SimpleDateFormat("EEEE, d MMM yyyy HH:mm", Locale.forLanguageTag("id-ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = intent.getStringExtra(EXTRA_TYPE) ?: Store.TYPE_IN
        b = ActivityAlarmSettingBinding.inflate(layoutInflater)
        setContentView(b.root)

        val isIn = type == Store.TYPE_IN
        val label = if (isIn) "masuk" else "pulang"
        b.toolbar.title = if (isIn) "Alarm Absen Masuk" else "Alarm Absen Pulang"
        b.toolbar.setNavigationOnClickListener { finish() }
        b.tvInfo.text = "Atur hingga 3 waktu alarm. Semua alarm absen $label otomatis OFF untuk hari itu " +
            "begitu Anda menekan \"Sudah absen $label\", dan aktif lagi esok harinya."

        for (slot in 0 until Store.SLOTS) {
            val sv = ItemAlarmSlotBinding.inflate(layoutInflater, b.slotsContainer, false)
            sv.tvSlotName.text = "ALARM ${slot + 1}"
            val pick = {
                TimePickerDialog(
                    this,
                    { _, h, m ->
                        Store.setSlotTime(this, type, slot, h, m)
                        // memilih jam otomatis mengaktifkan slot tersebut
                        Store.setSlotEnabled(this, type, slot, true)
                        Scheduler.scheduleAll(this)
                        refresh()
                    },
                    Store.slotHour(this, type, slot), Store.slotMinute(this, type, slot), true
                ).show()
            }
            sv.slotTimeArea.setOnClickListener { pick() }
            sv.switchSlot.setOnCheckedChangeListener { _, checked ->
                if (updating) return@setOnCheckedChangeListener
                Store.setSlotEnabled(this, type, slot, checked)
                Scheduler.scheduleAll(this)
                refresh()
            }
            b.slotsContainer.addView(sv.root)
            slotViews.add(sv)
        }

        b.switchAlarm.setOnCheckedChangeListener { _, checked ->
            if (updating) return@setOnCheckedChangeListener
            Store.setAlarmEnabled(this, type, checked)
            if (!checked && AlarmService.ringingType == type) AlarmService.stop(this)
            Scheduler.scheduleAll(this)
            refresh()
        }

        b.btnTest.setOnClickListener { AlarmService.start(this, type, test = true) }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val enabled = Store.alarmEnabled(this, type)
        val done = Store.isDone(this, type) && Store.isTodayEnabled(this)

        b.tvSchedInfo.text = (if (type == Store.TYPE_IN) "Jadwal masuk " else "Jadwal pulang ") +
            Store.scheduleLabel(this, type)

        updating = true
        b.switchAlarm.isChecked = enabled
        for (slot in 0 until Store.SLOTS) {
            val sv = slotViews[slot]
            sv.tvSlotTime.text = Store.slotLabel(this, type, slot)
            sv.switchSlot.isChecked = Store.slotEnabled(this, type, slot)
            sv.root.alpha = if (enabled) 1f else 0.5f
        }
        updating = false

        val (label, color) = when {
            !enabled -> "NONAKTIF" to R.color.text_secondary
            done -> "OFF OTOMATIS • SUDAH ABSEN" to R.color.green
            else -> "AKTIF" to R.color.blue
        }
        b.tvStatus.text = label
        b.tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, color))

        val next = Scheduler.nextAlarmMillis(this, type)
        b.tvNext.text = if (next != null) {
            "Berbunyi berikutnya: " + nextFmt.format(Date(next))
        } else {
            "Tidak ada jadwal alarm (alarm nonaktif, tidak ada slot aktif, atau tidak ada hari yang dipilih)."
        }
    }
}
