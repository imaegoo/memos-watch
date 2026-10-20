package com.imaegoo.memoswatch

import android.content.Context
import android.util.TypedValue
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

fun Context.dp(value: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()

fun relativeTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - epochMillis
    if (diff < 0) return "刚刚"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "$minutes 分钟前"
        hours < 24 -> "$hours 小时前"
        days < 7 -> "$days 天前"
        days < 365 -> {
            val d = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            "%d月%d日".format(d.monthValue, d.dayOfMonth)
        }
        else -> {
            val d = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            "${d.year}-${"%02d".format(d.monthValue)}-${"%02d".format(d.dayOfMonth)}"
        }
    }
}

private val fullFormatter: DateTimeFormatter by lazy {
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
}

fun fullTime(epochMillis: Long): String =
    if (epochMillis <= 0L) "" else fullFormatter.format(Instant.ofEpochMilli(epochMillis))
