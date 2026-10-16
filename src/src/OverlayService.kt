package com.aim.ipadview

import android.app.*
import android.content.*
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.*
import android.widget.TextView

class CrossView(c: Context) : View(c) {
    private val p = c.getSharedPreferences("cfg", Context.MODE_PRIVATE)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val l = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> invalidate() }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); p.registerOnSharedPreferenceChangeListener(l) }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); p.unregisterOnSharedPreferenceChangeListener(l) }
    override fun onDraw(c: Canvas) {
        val d = resources.displayMetrics.density
        val s = p.getInt("size", 12) * d
        paint.color = Color.argb(p.getInt("a", 255), p.getInt("r", 255), p.getInt("g", 0), p.getInt("b", 0))
        paint.strokeWidth = (p.getInt("thick", 3).coerceAtLeast(1)) * d
        val cx = width / 2f + (p.getInt("ox", 100) - 100) * d
        val cy = height / 2f + (p.getInt("oy", 100) - 100) * d
        when (p.getInt("shape", 0)) {
            0 -> { c.drawLine(cx - s, cy - s, cx + s, cy + s, paint); c.drawLine(cx - s, cy + s, cx + s, cy - s, paint) }
            1 -> { c.drawLine(cx - s, cy, cx + s, cy, paint); c.drawLine(cx, cy - s, cx, cy + s, paint) }
            else -> { paint.style = Paint.Style.FILL; c.drawCircle(cx, cy, s / 3, paint) }
        }
        paint.style = Paint.Style.STROKE
    }
}

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var cross: CrossView? = null
    private var fab: TextView? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        if (cross != null) return START_STICKY
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("ov", "Overlay", NotificationManager.IMPORTANCE_LOW))
        startForeground(1, Notification.Builder(this, "ov")
            .setContentTitle("حاتم يعمل")
            .setSmallIcon(android.R.drawable.ic_menu_view).build())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val cv = CrossView(this)
        wm.addView(cv, WindowManager.LayoutParams(
            -1, -1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT))
        cross = cv

        val d = resources.displayMetrics.density
        val sz = (52 * d).toInt()
        val b = TextView(this).apply {
            text = "◎"; textSize = 22f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.argb(160, 0, 0, 0)) }
        }
        val lp = WindowManager.LayoutParams(sz, sz, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 20; y = 300 }
        val gd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                cv.visibility = if (cv.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
                return true
            }
            override fun onLongPress(e: MotionEvent) {
                startActivity(Intent(this@OverlayService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        })
        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f
        b.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; tx = e.rawX; ty = e.rawY }
                MotionEvent.ACTION_MOVE -> { lp.x = sx + (e.rawX - tx).toInt(); lp.y = sy + (e.rawY - ty).toInt(); wm.updateViewLayout(v, lp) }
            }
            gd.onTouchEvent(e); true
        }
        wm.addView(b, lp)
        fab = b
        return START_STICKY
    }

    override fun onDestroy() {
        cross?.let { wm.removeView(it) }
        fab?.let { wm.removeView(it) }
        cross = null; fab = null
        super.onDestroy()
    }
}

