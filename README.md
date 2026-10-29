# ASN ABSEN REMINDER (by RandyAa)

Aplikasi Android pengingat absen masuk dan absen pulang.

## Fitur

- **Jadwal default absen** (default masuk 07:30, pulang 16:30) bisa diubah dari layar utama (ketuk "Jadwal ... ✎ ubah") atau menu ⋮ → *Jadwal Default Absen*. Jadwal **terpisah dari alarm** dan tidak membunyikan alarm.
- **Alarm absen masuk: hingga 3 waktu** dan **alarm absen pulang: hingga 3 waktu** (menu ⋮ → Alarm Absen Masuk / Pulang). Setiap waktu punya sakelar sendiri. Begitu absen masuk dicatat, semua alarm masuk hari itu otomatis OFF; begitu absen pulang dicatat, semua alarm pulang hari itu otomatis OFF. Aktif lagi esok harinya.
- Default pengingat **Senin–Jumat**. Sabtu dan Minggu bisa diaktifkan lewat kartu *Hari Pengingat*.
- **Notifikasi status bar**: *Belum absen masuk* muncul sejak **00:01** sampai absen masuk; *Belum absen pulang* muncul sejak jadwal pulang + 1 menit (default 16:31) sampai absen pulang.
- **Layanan latar belakang**: berjalan otomatis saat HP menyala dan saat aplikasi dibuka/ditutup, agar notifikasi dan alarm tetap tepat waktu (bisa dimatikan di menu *Izin & Baterai*).
- Timestamp jam:menit:detik, tombol *Ubah jam*, reset data, riwayat 7 hari, kartu merah/hijau.

## Cara mendapatkan APK lewat GitHub (tanpa Android Studio)

1. Buat repository baru di GitHub, lalu upload seluruh isi folder proyek ini
   (pastikan folder `.github` ikut terupload).
2. Buka tab **Actions** → workflow **Build APK** → tunggu sampai selesai (sekitar 3–5 menit).
   Bisa juga dijalankan manual dengan tombol *Run workflow*.
3. Buka hasil run tersebut, unduh artifact **ASN-ABSEN-REMINDER-apk**, ekstrak, lalu pasang
   `app-debug.apk` di HP (izinkan "Install dari sumber tidak dikenal").

## Cara lain: Android Studio

Buka folder ini di Android Studio (Hedgehog atau lebih baru), tunggu Gradle sync, lalu
**Build → Build APK(s)**.

## Pengaturan penting di HP

Buka menu titik tiga → **Izin & Baterai**, lalu:

- Izinkan **notifikasi**.
- Pilih **tanpa pembatasan** pada optimasi baterai agar alarm tepat waktu.
- Android 14: izinkan **notifikasi layar penuh** agar layar alarm muncul di atas kunci layar.
- Beberapa merek HP (Xiaomi, Oppo, Vivo, Realme, Samsung) perlu mengaktifkan **Mulai otomatis / Autostart**
  untuk aplikasi ini di pengaturan sistem.

## Struktur singkat

| File | Fungsi |
|------|--------|
| `Store.kt` | Penyimpanan data absen, hari pengingat, pengaturan alarm |
| `Scheduler.kt` | Penjadwalan alarm 07:30 / 16:30 dan pemicu notifikasi 00:01 / 16:31 |
| `Notifier.kt` | Notifikasi status bar dan notifikasi alarm |
| `AlarmService.kt`, `AlarmActivity.kt` | Suara/getar dan layar alarm penuh |
| `Receivers.kt` | Penerima alarm, tombol notifikasi, dan boot |
| `MainActivity.kt`, `AlarmSettingActivity.kt` | Layar utama dan menu alarm |

## v1.2

- Nama aplikasi ASN ABSEN REMINDER, ikon jam baru, alarm 3 waktu, jadwal default terpisah, layanan latar belakang.

## v1.1 — Akun STAR ASN
- Menu ⋮ → **Akun STAR ASN**: simpan akun sendiri (password terenkripsi Android Keystore, disembunyikan, lihat butuh PIN/sidik jari HP).
- Tombol **BUKA STAR ASN** di layar utama, tombol di notifikasi pengingat, dan notifikasi "Login STAR ASN" yang muncul bersamaan dengan notifikasi belum absen (masuk/pulang) dan hilang otomatis setelah absen tercatat → membuka STAR ASN dengan login terisi otomatis.
- Kredensial hanya diisikan di domain `kemenimipas.go.id` (lihat `TRUSTED_HOSTS` di `StarAsnActivity.kt`).
- Tombol presensi tetap ditekan pengguna sendiri.
