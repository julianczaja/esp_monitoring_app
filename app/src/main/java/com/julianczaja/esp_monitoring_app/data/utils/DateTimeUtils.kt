package com.julianczaja.esp_monitoring_app.data.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

const val EXIF_UTC_OFFSET = "+00:00"

private val localDateTimeExifFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
private val localDateTimePrettyFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd, HH:mm:ss")
private val localDateTimeDefaultFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
    .appendValue(ChronoField.YEAR_OF_ERA, 4)
    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
    .appendValue(ChronoField.DAY_OF_MONTH, 2)
    .appendValue(ChronoField.HOUR_OF_DAY, 2)
    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
    .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
    .appendValue(ChronoField.MILLI_OF_SECOND, 3)
    .toFormatter()

private val localTimePrettyFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

fun LocalDateTime.toEpochMillis(): Long = toInstant(ZoneOffset.UTC).toEpochMilli()

fun Long.millisToDefaultFormatLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneOffset.UTC)

fun String.toDefaultFormatLocalDateTime(): LocalDateTime = LocalDateTime.parse(this, localDateTimeDefaultFormatter)

fun LocalDateTime.toDefaultFormatString(): String = localDateTimeDefaultFormatter.format(this)

fun LocalDateTime.toPrettyString(): String = this.format(localDateTimePrettyFormatter)

fun LocalDateTime.toExifString(): String = this.format(localDateTimeExifFormatter)

fun LocalTime.toPrettyString(): String = this.format(localTimePrettyFormatter)

fun LocalDate.toMonthDayString(): String = this.format(DateTimeFormatter.ofPattern("MMM, d"))

fun LocalDateTime.toDayMonthYearString(): String = this.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

object LocalDateTimeAsStringSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDateTimeAsStringSerializer", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toDefaultFormatString())
    override fun deserialize(decoder: Decoder): LocalDateTime = decoder.decodeString().toDefaultFormatLocalDateTime()
}
