package com.aim.ipadview

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object Sh {
    fun running(): Boolean = try { Shizuku.pingBinder() } catch (e: Throwable) { false }

    fun granted(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Throwable) { false }

    fun request() { try { Shizuku.requestPermission(1) } catch (e: Throwable) {} }

    // ينفذ أمراً بصلاحية shell عبر Shizuku ويرجع (رمز الخروج، النص)
    fun run(vararg args: String): Pair<Int, String> {
        return try {
            val m = Shizuku::class.java.getDeclaredMethod(
                "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            m.isAccessible = true
            val pr = m.invoke(null, arrayOf(*args), null, null) as Process
            val out = pr.inputStream.bufferedReader().readText()
            val err = pr.errorStream.bufferedReader().readText()
            val code = pr.waitFor()
            code to (out + err).trim()
        } catch (e: Throwable) {
            -1 to (e.cause?.toString() ?: e.toString())
        }
    }
}
