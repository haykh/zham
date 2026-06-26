package io.github.haykh.zham

import kotlinx.serialization.Serializable
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Preset patterns offered in the dropdown; the stored format can be any pattern string. */
val timeFormatPresets: List<String> =
    listOf(
        "HH:mm:ss",
        "HH:mm",
        "h:mm:ss a",
        "h:mm a",
    )

@Serializable
data class City(
    val label: String,
    val zoneIdName: String,
) {
    val zone: ZoneId
        get() = ZoneId.of(zoneIdName)
}

object Flags {
    /** Country flag emoji for a zone, or 🌐 for non-country zones (UTC, GMT, etc.). */
    fun emojiFor(zoneIdName: String): String {
        val region =
            android.icu.util.TimeZone
                .getRegion(zoneIdName) // ISO-3166 alpha-2, e.g. "AM"
        if (region.length != 2 || !region.all { it in 'A'..'Z' } || region == "ZZ") return "🌐"
        return region
            .map { 0x1F1E6 + (it - 'A') }
            .joinToString("") { String(Character.toChars(it)) }
    }
}

object Clocks {
    val cities: List<City> =
        listOf(
            City("New York", "America/New_York"),
            City("London", "Europe/London"),
            City("Yerevan", "Asia/Yerevan"),
            City("Tokyo", "Asia/Tokyo"),
            City("Sydney", "Australia/Sydney"),
        )

    fun format(
        city: City,
        now: ZonedDateTime,
        formatter: DateTimeFormatter,
    ): String = now.withZoneSameInstant(city.zone).format(formatter).lowercase()

    /** Build a formatter from a (possibly user-entered) pattern, falling back if invalid. */
    fun formatterOf(pattern: String): DateTimeFormatter =
        runCatching { DateTimeFormatter.ofPattern(pattern) }
            .getOrDefault(DateTimeFormatter.ofPattern("HH:mm:ss"))
}
