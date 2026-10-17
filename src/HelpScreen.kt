package com.aim.ipadview

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

object HelpScreen {
    private fun dp(a: Activity, v: Int) = (v * a.resources.displayMetrics.density).toInt()

    private fun box(a: Activity, hi: Boolean, bg: Int = Color.rgb(43, 45, 49)): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = dp(a, 10).toFloat()
            setColor(bg)
            if (hi) setStroke(dp(a, 3), Color.RED)
        }

    // صورة توضيحية لسطر في الإعدادات (toggle: -1 بدون مفتاح، 0 مطفأ، 1 مشغل)
    private fun row(a: Activity, title: String, sub: String?, toggle: Int, hi: Boolean): View {
        val r = LinearLayout(a).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(a, 14), dp(a, 12), dp(a, 14), dp(a, 12))
            background = box(a, hi)
        }
        val col = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(a).apply { text = title; textSize = 16f; setTextColor(Color.WHITE) })
        if (sub != null) col.addView(TextView(a).apply { text = sub; textSize = 12f; setTextColor(Color.LTGRAY) })
        r.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        if (toggle >= 0) r.addView(Switch(a).apply { isChecked = toggle == 1; isClickable = false })
        if (hi) r.addView(TextView(a).apply { text = "  👈 هنا"; setTextColor(Color.RED); textSize = 14f })
        return r
    }

    private fun term(a: Activity, t: String): View = TextView(a).apply {
        text = t
        typeface = Typeface.MONOSPACE
        textSize = 13f
        setTextColor(Color.rgb(80, 255, 80))
        setPadding(dp(a, 12), dp(a, 12), dp(a, 12), dp(a, 12))
        background = box(a, false, Color.BLACK)
        textDirection = View.TEXT_DIRECTION_LTR
    }

    private fun heading(a: Activity, p: LinearLayout, t: String) {
        p.addView(TextView(a).apply {
            text = t; textSize = 21f; setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE); setPadding(0, dp(a, 28), 0, dp(a, 4))
        })
    }

    private fun para(a: Activity, p: LinearLayout, t: String) {
        p.addView(TextView(a).apply {
            text = t; textSize = 15f; setTextColor(Color.LTGRAY); setPadding(0, dp(a, 6), 0, dp(a, 6))
        })
    }

    private fun step(a: Activity, p: LinearLayout, n: Int, title: String, desc: String, vararg mocks: View) {
        p.addView(TextView(a).apply {
            text = "الخطوة $n: $title"; textSize = 17f; setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(255, 180, 60)); setPadding(0, dp(a, 22), 0, dp(a, 4))
        })
        p.addView(TextView(a).apply {
            text = desc; textSize = 15f; setTextColor(Color.WHITE); setPadding(0, 0, 0, dp(a, 8))
        })
        for (m in mocks) p.addView(m, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(a, 6) })
    }

    fun show(a: Activity) {
        val d = Dialog(a, android.R.style.Theme_DeviceDefault)
        val root = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(a, 18), dp(a, 18), dp(a, 18), dp(a, 60))
        }
        root.addView(Button(a).apply { text = "إغلاق"; setOnClickListener { d.dismiss() } })

        root.addView(TextView(a).apply {
            text = "؟ شرح الأذونات"; textSize = 28f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); setPadding(0, dp(a, 10), 0, dp(a, 6))
        })
        para(a, root, "الصور في هذه الصفحة رسومات توضيحية تقريبية، وشكل الإعدادات يختلف قليلاً من هاتف لآخر. الإطار الأحمر 👈 يدل على ما تضغط عليه.")

        heading(a, root, "ماذا يطلب حاتم ولماذا؟")
        para(a, root, "1) العرض فوق التطبيقات: لإظهار علامة التصويب والأيقونة العائمة فوق اللعبة.")
        para(a, root, "2) السماح عبر تطبيق Shizuku المساعد: ليغيّر حاتم حجم الشاشة (الكثافة) فقط، وهذا هو منظور الآيباد. أندرويد لا يسمح بذلك لأي تطبيق وحده.")
        para(a, root, "3) الإشعارات: ليبقى حاتم يعمل في الخلفية أثناء اللعب.")
        para(a, root, "حاتم لا يملك صلاحية الإنترنت أصلاً، فلا يرسل أي شيء من هاتفك.")

        heading(a, root, "أولاً: العرض فوق التطبيقات")
        step(a, root, 1, "اسمح لحاتم",
            "عند أول ضغطة على تفعيل تفتح لك هذه الشاشة تلقائياً. فعّل المفتاح بجانب حاتم ثم ارجع للتطبيق واضغط تفعيل مرة ثانية.",
            row(a, "حاتم", "مسموح بالعرض فوق التطبيقات الأخرى", 1, true))

        heading(a, root, "ثانياً: منظور الآيباد عبر Shizuku")
        para(a, root, "تحتاج Wi-Fi شغال، وخيارات المطور مفعلة والتصحيح اللاسلكي شغال (فعلتها سابقاً).")

        step(a, root, 1, "ثبّت تطبيق Shizuku",
            "حمّل Shizuku من متجر Play. هو الذي يمنح حاتم إذن تغيير حجم الشاشة.",
            row(a, "Shizuku", "متجر Play", -1, false))

        step(a, root, 2, "اقتران Shizuku",
            "افتح Shizuku واضغط على \"Start via Wireless debugging\" ثم زر \"Pairing\". سيظهر إشعار فيه خانة لكتابة رمز الاقتران. افتح صفحة \"اقتران الجهاز برمز الاقتران\" في إعدادات التصحيح اللاسلكي واكتب الرمز في خانة الإشعار.",
            row(a, "Start via Wireless debugging", "Pairing", -1, true),
            row(a, "إشعار Shizuku: اكتب رمز الاقتران", "123456", -1, false))

        step(a, root, 3, "شغّل Shizuku",
            "بعد نجاح الاقتران اضغط \"Start\". تظهر عبارة Shizuku is running عند النجاح.",
            row(a, "Shizuku is running", null, -1, true))

        step(a, root, 4, "اسمح لحاتم",
            "ارجع لحاتم واضغط \"السماح لحاتم\"، ثم اضغط \"السماح\" في نافذة Shizuku.",
            row(a, "حاتم يطلب استخدام Shizuku", "السماح", -1, true))

        step(a, root, 5, "تأكد",
            "تظهر في حاتم عبارة \"منظور الآيباد جاهز ✓\" وبعدها يعمل الشريط مباشرة.",
            row(a, "منظور الآيباد جاهز ✓", null, -1, false))

        para(a, root, "ملاحظة: بعد إعادة تشغيل الهاتف افتح Shizuku واضغط Start من جديد (يحتاج Wi-Fi). الاقتران يبقى محفوظاً. ومنظور الآيباد الذي ضبطته يبقى مطبقاً حتى تضغط إيقاف.")

        root.addView(Button(a).apply { text = "إغلاق"; setOnClickListener { d.dismiss() } },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(a, 24) })

        d.setContentView(ScrollView(a).apply { addView(root); setBackgroundColor(Color.rgb(28, 29, 33)) })
        d.window?.setLayout(-1, -1)
        d.show()
    }
}

