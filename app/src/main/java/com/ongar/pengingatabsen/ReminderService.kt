package com.ongar.pengingatabsen

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Layanan latar belakang: menjaga proses aplikasi tetap hidup (notifikasi senyap berprioritas
 * minimum) dan menyamakan notifikasi "belum absen" tiap menit, serta memasang ulang jadwal
 * alarm tiap 15 menit. Dijalankan otomatis saat HP menyala, aplikasi diperbarui, dan aplikasi dibuka.
 */
class ReminderService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var ticks = 0

    private val tick = object : Runnable {
        override fun run() {
            Notifier.sync(this@ReminderService)
            ticks++
            if (ticks % 15 == 0) Scheduler.scheduleAll(this@ReminderService)
            handler.postDelayed(this, 60_000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Store.bgServiceEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        Notifier.createChannels(this)
        val n = notification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(Notifier.ID_BG, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Notifier.ID_BG, n)
        }
        Scheduler.scheduleAll(this)
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Aplikasi disapu dari daftar terbaru: layanan tetap jalan, jadwal dipasang ulang untuk berjaga-jaga
        Scheduler.scheduleAll(this)
        Notifier.sync(this)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        // Bila layanan dimatikan sistem (bukan oleh pengguna), minta dihidupkan lagi beberapa detik kemudian
        if (Store.bgServiceEnabled(this)) Scheduler.kick(this)
        super.onDestroy()
    }

    companion object {
        private fun notification(ctx: Context): Notification {
            val open = PendingIntent.getActivity(
                ctx, 73,
                Intent(ctx, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return NotificationCompat.Builder(ctx, Notifier.CH_BG)
                .setSmallIcon(R.drawable.ic_stat_clock)
                .setContentTitle("ASN ABSEN REMINDER aktif")
                .setContentText("Berjalan di latar belakang agar pengingat tepat waktu")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setSilent(true)
                .setShowWhen(false)
                .setContentIntent(open)
                .build()
        }

        /** Menghidupkan layanan bila diizinkan; aman dipanggil berulang dan dari mana saja. */
        fun ensure(ctx: Context) {
            val app = ctx.applicationContext
            if (!Store.bgServiceEnabled(app)) return
            try {
                ContextCompat.startForegroundService(app, Intent(app, ReminderService::class.java))
            } catch (e: Exception) {
                // Sistem melarang start dari latar belakang saat ini; akan dicoba lagi oleh alarm berikutnya
            }
        }

        fun stop(ctx: Context) {
            val app = ctx.applicationContext
            app.stopService(Intent(app, ReminderService::class.java))
        }
    }
}
