package com.aim.ipadview

import android.app.Activity
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private val games = listOf(
        "PUBG Mobile" to "com.tencent.ig",
        "PUBG Mobile (Korea)" to "com.pubg.krmobile",
        "BGMI" to "com.pubg.imobile",
        "PUBG Mobile Lite" to "com.tencent.iglite",
        "Free Fire" to "com.dts.freefireth",
        "Call of Duty Mobile" to "com.activision.callofduty.shooter"
    )
    private lateinit var p: SharedPreferences
    private lateinit var preview: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        p = getSharedPreferences("cfg", MODE_PRIVATE)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 80) }
        setContentView(ScrollView(this).apply { addView(root) })

        fun label(t: String) { root.addView(TextView(this).apply { text = t; textSize = 16f; setPadding(0, 30, 0, 5) }) }
        fun button(t: String, f: () -> Unit) { root.addView(Button(this).apply { text = t; setOnClickListener { f() } }) }
        fun seek(key: String, mx: Int, df: Int) {
            root.addView(SeekBar(this).apply {
                max = mx; progress = p.getInt(key, df)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) { p.edit().putInt(key, v).apply(); refresh() }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            })
        }

        label("اختر اللعبة / Game")
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, games.map { it.first })
            setSelection(p.getInt("game", 0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(a: AdapterView<*>?, v: View?, pos: Int, id: Long) { p.edit().putInt("game", pos).apply() }
                override fun onNothingSelected(a: AdapterView<*>?) {}
            }
        })

        button("تفعيل / Activate") { activate() }
        button("إيقاف / Stop") { stopService(Intent(this, OverlayService::class.java)) }

        label("علامة التصويب / Crosshair")
        preview = TextView(this).apply { text = "██████████"; textSize = 28f; setBackgroundColor(Color.DKGRAY); gravity = android.view.Gravity.CENTER }
        root.addView(preview)
        label("أحمر R"); seek("r", 255, 255)
        label("أخضر G"); seek("g", 255, 0)
        label("أزرق B"); seek("b", 255, 0)
        label("الشفافية Opacity"); seek("a", 255, 255)
        label("الحجم Size"); seek("size", 40, 12)
        label("السماكة Thickness"); seek("thick", 10, 3)
        label("الموضع أفقي X (100 = المنتصف)"); seek("ox", 200, 100)
        label("الموضع عمودي Y (100 = المنتصف)"); seek("oy", 200, 100)
        button("تغيير الشكل X / + / نقطة") { p.edit().putInt("shape", (p.getInt("shape", 0) + 1) % 3).apply() }

        label("منظور الآيباد / iPad view")
        val ok = checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == android.content.pm.PackageManager.PERMISSION_GRANTED
        root.addView(TextView(this).apply {
            text = if (ok) "الصلاحية مفعلة ✓" else "الصلاحية غير مفعلة ✗ (اضغط الزر أدناه لمعرفة الأمر)"
            setTextColor(if (ok) Color.rgb(0, 160, 0) else Color.RED)
        })
        if (!ok) button("نسخ أمر منح الصلاحية") {
            val cmd = "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("cmd", cmd))
            Toast.makeText(this, "تم النسخ. الصقه في LADB أو أي ADB على الهاتف", Toast.LENGTH_LONG).show()
        }
        if (p.getInt("origDpi", 0) == 0) p.edit().putInt("origDpi", resources.displayMetrics.densityDpi).apply()
        val orig = p.getInt("origDpi", 400)
        val info = TextView(this).apply { textSize = 18f; gravity = android.view.Gravity.CENTER }
        root.addView(info)
        val bar = SeekBar(this).apply { max = 70; progress = p.getInt("pct", 100) - 50 }
        fun show() { val pct = bar.progress + 50; info.text = "الابتعاد: $pct%  (dpi ${orig * pct / 100})" }
        fun applyNow() {
            val pct = bar.progress + 50
            p.edit().putInt("pct", pct).apply()
            val dpi = orig * pct / 100
            Thread {
                val good = try { Runtime.getRuntime().exec(arrayOf("wm", "density", dpi.toString())).waitFor() == 0 } catch (e: Exception) { false }
                runOnUiThread { if (!good) Toast.makeText(this, "فشل التطبيق: تأكد من منح الصلاحية", Toast.LENGTH_LONG).show() }
            }.start()
        }
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) { show() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) { applyNow() }
        })
        root.addView(bar)
        show()
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(Button(this).apply { text = "−"; textSize = 22f; setOnClickListener { bar.progress = (bar.progress - 2).coerceAtLeast(0); applyNow() } },
            LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply { text = "+"; textSize = 22f; setOnClickListener { bar.progress = (bar.progress + 2).coerceAtMost(70); applyNow() } },
            LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(row)
        button("إعادة الوضع الأصلي 100%") { bar.progress = 50; applyNow() }
        label("اسحب الشريط لتقليل % فتبتعد الشخصية، والحواف تبقى كما هي. ابدأ بـ 85% وجرب.")
        button("(بديل) فتح خيارات المطور Smallest width") {
            try { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
            catch (e: Exception) { Toast.makeText(this, "فعّل خيارات المطور أولاً", Toast.LENGTH_LONG).show() }
        }

        label("عكس الألوان / Invert colors")
        button("تشغيل/إيقاف عكس الألوان") { toggleInversion() }
        refresh()
    }

    private fun refresh() {
        preview.setTextColor(Color.argb(p.getInt("a", 255), p.getInt("r", 255), p.getInt("g", 0), p.getInt("b", 0)))
    }

    private fun activate() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "اسمح بالعرض فوق التطبيقات ثم ارجع واضغط تفعيل", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        startForegroundService(Intent(this, OverlayService::class.java))
        val pkg = games[p.getInt("game", 0)].second
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) startActivity(launch)
        else Toast.makeText(this, "اللعبة غير مثبتة", Toast.LENGTH_LONG).show()
    }

    private fun toggleInversion() {
        val key = "accessibility_display_inversion_enabled"
        try {
            val on = Settings.Secure.getInt(contentResolver, key, 0) == 1
            Settings.Secure.putInt(contentResolver, key, if (on) 0 else 1)
        } catch (e: Exception) {
            try { startActivity(Intent("android.settings.COLOR_INVERSION_SETTINGS")) }
            catch (e2: Exception) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
    }
}
