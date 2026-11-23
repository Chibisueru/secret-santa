package io.github.chibisueru.secretsanta.domain

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The date picker stores dates as UTC midnight, so format in UTC to get the same day back. */
fun formatEventDate(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
