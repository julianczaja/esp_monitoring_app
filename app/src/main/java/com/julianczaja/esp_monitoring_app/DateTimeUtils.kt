package com.julianczaja.esp_monitoring_app

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.*

private val prettyFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")

private val defaultFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
    .appendValue(ChronoField.YEAR_OF_ERA, 4)
    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
    .appendValue(ChronoField.DAY_OF_MONTH, 2)
    .appendValue(ChronoField.HOUR_OF_DAY, 2)
    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
    .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
    .appendValue(ChronoField.MILLI_OF_SECOND, 3)
    .toFormatter()

fun String.toDefaultFormatLocalDateTime(): LocalDateTime = LocalDateTime.parse(this, defaultFormatter)

fun LocalDateTime.toDefaultFormatString(): String = defaultFormatter.format(this)

fun LocalDateTime.toPrettyString(): String = this.format(prettyFormatter)

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = LocalDateTime::class)
object LocalDateTimeAsStringSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDateTimeAsStringSerializer", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toDefaultFormatString())
    override fun deserialize(decoder: Decoder): LocalDateTime = decoder.decodeString().toDefaultFormatLocalDateTime()
}
