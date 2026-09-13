package com.example.cpen321application.util

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

fun formatTimeWithGmtOffset(value: String): String =
    formatTimeWithGmtOffset(ZonedDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME))

fun formatTimeWithGmtOffset(value: ZonedDateTime): String =
    value.format(TIME_WITH_GMT_OFFSET_FORMATTER)

fun normalizeIpAddress(ipAddress: String): String =
    ipAddress.trim().removePrefix("::ffff:")

private val TIME_WITH_GMT_OFFSET_FORMATTER = DateTimeFormatterBuilder()
    .appendPattern("HH:mm:ss 'GMT'")
    .appendOffset("+HH:MM", "+00:00")
    .toFormatter()
