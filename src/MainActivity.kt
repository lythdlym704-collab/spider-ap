package com.aim.ipadview

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
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
    private lateinit var bar: SeekBar
    private lateinit var info: TextView
    private var orig = 400

    private fun hasPerm() = checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        p = getSharedPreferences("cfg", MODE_PRIVATE)
        if (p.getInt("origDpi", 0) == 0) p.edit().putInt("origDpi", resources.displayMetrics.densityDpi).apply()
        orig = p.getInt("origDpi", 400)

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

        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(this).apply { text = "حاتم"; textSize = 30f; gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(Button(this).apply { text = "؟"; textSize = 22f; setOnClickListener { HelpScreen.show(this@MainActivity) } })
        root.addView(top)

        label("اختر اللعبة")
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, games.map { it.first })
            setSelection(p.getInt("game", 0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(a: AdapterView<*>?, v: View?, pos: Int, id: Long) { p.edit().putInt("game", pos).apply() }
                override fun onNothingSelected(a: AdapterView<*>?) {}
            }
        })

        button("▶ تفعيل (يفتح اللعبة + منظور الآيباد + العلامة)") { activate() }
        button("■ إيقاف (يرجع كل شيء كما كان)") {
            stopService(Intent(this, OverlayService::class.java))
            if (hasPerm()) wmDensity("reset")
        }

        label("منظور الآيباد")
        val st = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        st.addView(TextView(this).apply {
            if (hasPerm()) { text = "الصلاحية مفعلة ✓"; setTextColor(Color.rgb(0, 160, 0)) }
            else { text = "الصلاحية غير مفعلة ✗ (مرة واحدة فقط)"; setTextColor(Color.RED) }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        st.addView(Button(this).apply { text = "؟"; setOnClickListener { HelpScreen.show(this@MainActivity) } })
        root.addView(st)
        if (!hasPerm()) {
            button("1) نسخ أمر الصلاحية") {
                val cmd = "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("cmd", cmd))
                Toast.makeText(this, "تم النسخ. الصقه في LADB ونفذه", Toast.LENGTH_LONG).show()
            }
            button("2) فتح خيارات المطور (التصحيح اللاسلكي)") {
                try { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
                catch (e: Exception) { Toast.makeText(this, "فعّل خيارات المطور أولاً", Toast.LENGTH_LONG).show() }
            }
        }
        info = TextView(this).apply { textSize = 18f; gravity = Gravity.CENTER }
        root.addView(info)
        bar = SeekBar(this).apply { max = 70; progress = p.getInt("pct", 85) - 50 }
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) { showPct() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) { applyPct() }
        })
        root.addView(bar)
        showPct()
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(Button(this).apply { text = "−"; textSize = 22f; setOnClickListener { bar.progress = (bar.progress - 2).coerceAtLeast(0); applyPct() } }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply { text = "+"; textSize = 22f; setOnClickListener { bar.progress = (bar.progress + 2).coerceAtMost(70); applyPct() } }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(row)
        label("قلل النسبة لتبتعد الشخصية. الحواف تبقى كما هي.")

        label("علامة التصويب")
        preview = TextView(this).apply { text = "██████████"; textSize = 28f; setBackgroundColor(Color.DKGRAY); gravity = Gravity.CENTER }
        root.addView(preview)
        label("أحمر"); seek("r", 255, 255)
        label("أخضر"); seek("g", 255, 0)
        label("أزرق"); seek("b", 255, 0)
        label("الشفافية"); seek("a", 255, 255)
        label("الحجم"); seek("size", 40, 12)
        label("السماكة"); seek("thick", 10, 3)
        label("الموضع الأفقي (100 = المنتصف)"); seek("ox", 200, 100)
        label("الموضع العمودي (100 = المنتصف)"); seek("oy", 200, 100)
        button("تغيير الشكل  X / + / نقطة") { p.edit().putInt("shape", (p.getInt("shape", 0) + 1) % 3).apply() }
        refresh()
    }

    private fun showPct() { val pct = bar.progress + 50; info.text = "الابتعاد: $pct%" }

    private fun applyPct() {
        val pct = bar.progress + 50
        p.edit().putInt("pct", pct).apply()
        if (!hasPerm()) { Toast.makeText(this, "فعّل الصلاحية أولاً (اضغط ؟ للشرح)", Toast.LENGTH_LONG).show(); return }
        wmDensity((orig * pct / 100).toString())
    }

    private fun wmDensity(arg: String) {
        Thread {
            val good = try { Runtime.getRuntime().exec(arrayOf("wm", "density", arg)).waitFor() == 0 } catch (e: Exception) { false }
            if (!good) runOnUiThread { Toast.makeText(this, "فشل تغيير المنظور", Toast.LENGTH_LONG).show() }
        }.start()
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
        if (hasPerm()) applyPct()
        val launch = packageManager.getLaunchIntentForPackage(games[p.getInt("game", 0)].second)
        if (launch != null) startActivity(launch)
        else Toast.makeText(this, "اللعبة غير مثبتة", Toast.LENGTH_LONG).show()
    }
}

