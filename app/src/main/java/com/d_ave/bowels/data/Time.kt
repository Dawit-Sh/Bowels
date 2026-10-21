package com.d_ave.bowels.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Timestamps are stored the way JavaScript's toISOString() wrote them, so old and new rows compare equal. */
private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

fun Instant.toIsoString(): String = isoFormatter.format(this)

fun parseInstant(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value) }
        .recoverCatching { OffsetDateTime.parse(value).toInstant() }
        .recoverCatching { LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant() }
        .getOrNull()
}

fun Instant.localDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate = atZone(zone).toLocalDate()

fun today(): LocalDate = LocalDate.now()
