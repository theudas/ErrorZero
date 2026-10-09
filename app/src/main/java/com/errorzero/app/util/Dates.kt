package com.errorzero.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object Dates {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    private val fullDate = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
    private val dateTime = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA)
    private val shortDate = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
    private val shortDateTime = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)
    private val todayFmt = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)
    private val fileStamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm", Locale.ROOT)

    fun toLocalDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    fun toLocalDateTime(ms: Long): LocalDateTime = Instant.ofEpochMilli(ms).atZone(zone).toLocalDateTime()
    fun toMillis(dt: LocalDateTime): Long = dt.atZone(zone).toInstant().toEpochMilli()

    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()
    fun startOfDayPlus(now: Long, days: Int): Long = startOfDay(toLocalDate(now).plusDays(days.toLong()))
    fun startOfToday(): Long = startOfDay(LocalDate.now(zone))
    fun endOfToday(): Long = startOfDay(LocalDate.now(zone).plusDays(1)) - 1
    fun endOfDayAfter(days: Int): Long = startOfDay(LocalDate.now(zone).plusDays(days + 1L)) - 1

    fun daysFromToday(ms: Long): Long = ChronoUnit.DAYS.between(LocalDate.now(zone), toLocalDate(ms))

    fun date(ms: Long): String = fullDate.format(toLocalDate(ms))
    fun dateTime(ms: Long): String = dateTime.format(toLocalDateTime(ms))
    fun shortDate(ms: Long): String {
        val d = toLocalDate(ms)
        return if (d.year == LocalDate.now(zone).year) shortDate.format(d) else fullDate.format(d)
    }
    fun shortDateTime(ms: Long): String {
        val dt = toLocalDateTime(ms)
        return if (dt.year == LocalDate.now(zone).year) shortDateTime.format(dt) else dateTime.format(dt)
    }
    fun todayLabel(): String = todayFmt.format(LocalDate.now(zone))
    fun fileStamp(): String = fileStamp.format(LocalDateTime.now(zone))

    fun dueLabel(next: Long): String = when (val d = daysFromToday(next)) {
        in Long.MIN_VALUE..-1 -> "逾期 ${-d} 天"
        0L -> "今日重做"
        1L -> "明天"
        in 2..6 -> "$d 天后"
        else -> shortDate(next)
    }

    // Material DatePicker 以 UTC 零点表示日期
    fun toPickerMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    fun fromPickerMillis(utc: Long): LocalDate = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
}
