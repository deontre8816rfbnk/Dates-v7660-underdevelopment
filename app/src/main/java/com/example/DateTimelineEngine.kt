package com.example

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

// Holds the computed high-precision time difference between two dates
data class TimeVectorResult(
    val isNegative: Boolean,
    
    // Grand Totals (Absolute values)
    val totalYears: Double,
    val totalMonths: Double,
    val totalWeeks: Double,
    val totalDays: Long,
    val totalHours: Long,
    val totalSeconds: Long,
    val totalMillis: Long,
    
    // Human Readable Breakdown (Absolute values)
    val breakdownYears: Long,
    val breakdownMonths: Int,
    val breakdownDays: Int,
    val breakdownHours: Int,
    val breakdownMinutes: Int,
    val breakdownSeconds: Int,
    val breakdownMillis: Int
)

object DateTimelineEngine {

    // Helper to format digit input into slash date string 'DD / MM / YYYY  HH:mm'
    fun formatInput(text: String, previousText: String): String {
        var processedText = text
        
        // If the user deleted a delimiter (space, slash, colon), also remove the preceding digit
        if (text.length < previousText.length) {
            val deletedPart = previousText.substring(text.length)
            if (deletedPart.contains("/") || deletedPart.contains(" ") || deletedPart.contains(":")) {
                val digitsOnly = text.filter { it.isDigit() }
                if (digitsOnly.isNotEmpty()) {
                    processedText = digitsOnly.dropLast(1)
                }
            }
        }
        
        val digits = processedText.filter { it.isDigit() }.take(12) // Limit to DDMMYYYYHHMM
        val sb = java.lang.StringBuilder()
        for (i in digits.indices) {
            sb.append(digits[i])
            if (i == 1) {
                sb.append(" / ")
            } else if (i == 3) {
                sb.append(" / ")
            } else if (i == 7) {
                if (digits.length > 8) {
                    sb.append("  ") // double space separator for time
                }
            } else if (i == 9 && digits.length > 10) {
                sb.append(":")
            }
        }
        return sb.toString()
    }

    // Flexible date and time parser that handles leap years, clamps days and outputs LocalDateTime
    fun parseChronosDate(input: String): LocalDateTime? {
        val digits = input.filter { it.isDigit() }
        if (digits.length < 8) return null // Need at least DDMMYYYY
        
        val day = digits.substring(0, 2).toIntOrNull() ?: return null
        val month = digits.substring(2, 4).toIntOrNull() ?: return null
        val year = digits.substring(4, 8).toIntOrNull() ?: return null
        
        val hour = if (digits.length >= 10) digits.substring(8, 10).toIntOrNull() ?: 0 else 0
        val minute = if (digits.length >= 12) digits.substring(10, 12).toIntOrNull() ?: 0 else 0
        
        return try {
            val validatedMonth = month.coerceIn(1, 12)
            val maxDays = when (validatedMonth) {
                4, 6, 9, 11 -> 30
                2 -> if (isLeapYear(year)) 29 else 28
                else -> 31
            }
            val validatedDay = day.coerceIn(1, maxDays)
            val validatedHour = hour.coerceIn(0, 23)
            val validatedMinute = minute.coerceIn(0, 59)
            
            LocalDateTime.of(year, validatedMonth, validatedDay, validatedHour, validatedMinute, 0)
        } catch (e: Exception) {
            null
        }
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    // High precision duration calculation sequentialized to handle lengths of different calendar units correctly
    fun computeTimeVector(birth: LocalDateTime, target: LocalDateTime): TimeVectorResult {
        val isNegative = target.isBefore(birth)
        val first = if (isNegative) target else birth
        val second = if (isNegative) birth else target

        // Sequential calculation to keep calendar unit boundaries intact
        var temp = first
        val years = temp.until(second, ChronoUnit.YEARS)
        temp = temp.plusYears(years)

        val months = temp.until(second, ChronoUnit.MONTHS)
        temp = temp.plusMonths(months)

        val days = temp.until(second, ChronoUnit.DAYS)
        temp = temp.plusDays(days)

        val hours = temp.until(second, ChronoUnit.HOURS)
        temp = temp.plusHours(hours)

        val minutes = temp.until(second, ChronoUnit.MINUTES)
        temp = temp.plusMinutes(minutes)

        val seconds = temp.until(second, ChronoUnit.SECONDS)
        temp = temp.plusSeconds(seconds)

        val millis = temp.until(second, ChronoUnit.MILLIS)

        // Grand Totals calculations
        val totalDaysCount = ChronoUnit.DAYS.between(first, second)
        val totalSecsCount = ChronoUnit.SECONDS.between(first, second)
        val totalMillisCount = ChronoUnit.MILLIS.between(first, second)
        val totalHoursCount = ChronoUnit.HOURS.between(first, second)
        
        // precise averages for continuous timeline vectors
        val totalYearsDouble = totalDaysCount.toDouble() / 365.2425
        val totalMonthsDouble = totalDaysCount.toDouble() / 30.437 / if (isNegative) -1 else 1
        val totalWeeksDouble = totalDaysCount.toDouble() / 7.0

        return TimeVectorResult(
            isNegative = isNegative,
            totalYears = if (isNegative) -totalYearsDouble else totalYearsDouble,
            totalMonths = if (isNegative) -Math.abs(totalMonthsDouble) else Math.abs(totalMonthsDouble),
            totalWeeks = if (isNegative) -totalWeeksDouble else totalWeeksDouble,
            totalDays = if (isNegative) -totalDaysCount else totalDaysCount,
            totalHours = if (isNegative) -totalHoursCount else totalHoursCount,
            totalSeconds = if (isNegative) -totalSecsCount else totalSecsCount,
            totalMillis = if (isNegative) -totalMillisCount else totalMillisCount,
            
            breakdownYears = years,
            breakdownMonths = months.toInt(),
            breakdownDays = days.toInt(),
            breakdownHours = hours.toInt(),
            breakdownMinutes = minutes.toInt(),
            breakdownSeconds = seconds.toInt(),
            breakdownMillis = millis.toInt()
        )
    }

    // List of deterministic historic timeline event anchors for the Negative Zone (before Birthdate)
    private val historicalAnchors = listOf(
        HistoricalEvent(1776, "The United States is declaring independence today."),
        HistoricalEvent(1789, "The French Revolution is erupting. The Bastille falls today."),
        HistoricalEvent(1804, "Napoleon Bonaparte is being crowned Emperor of the French today."),
        HistoricalEvent(1815, "Napoleon is meeting defeat at the Battle of Waterloo today."),
        HistoricalEvent(1859, "Darwin is publishing 'On the Origin of Species' today."),
        HistoricalEvent(1865, "The American Civil War has ended today. A nation heals."),
        HistoricalEvent(1879, "Edison is lighting up the world with his first incandescent bulb today."),
        HistoricalEvent(1889, "The Eiffel Tower is opening in Paris today, climbing into the sky."),
        HistoricalEvent(1903, "The Wright brothers are completing their historic first flight in Kitty Hawk today."),
        HistoricalEvent(1912, "The Titanic is sinking in the icy depths of the North Atlantic today."),
        HistoricalEvent(1914, "World War I has been ignited. Shadows gather across Europe today."),
        HistoricalEvent(1918, "The guns fall silent. World War I ends with the Compiègne armistice today."),
        HistoricalEvent(1928, "Alexander Fleming is discovering penicillin today, changing medicine forever."),
        HistoricalEvent(1939, "World War II is beginning today. Darkness looms across the globe."),
        HistoricalEvent(1945, "World War II ends today. A fragile peace returns to humanity."),
        HistoricalEvent(1953, "DNA's double helix is discovered today, unraveling the key to life."),
        HistoricalEvent(2021, "Sputnik is ascending (actually Sputnik was 1957, let's keep it accurate):"),
        HistoricalEvent(1957, "Sputnik 1, the first satellite, is orbiting Earth today as the Space Age begins."),
        HistoricalEvent(1963, "Martin Luther King Jr. is speaking his dream on the Washington steps today."),
        HistoricalEvent(1969, "Apollo 11 lands on the Moon today. Humanity steps onto another celestial world."),
        HistoricalEvent(1977, "Star Wars is releasing in theaters today, forging a new cinematic mythos."),
        HistoricalEvent(1989, "The Berlin Wall is crumbling today, reuniting families and dividing epochs."),
        HistoricalEvent(1991, "The World Wide Web is released to the public today, uniting the global matrix."),
        HistoricalEvent(2001, "The world changes forever today. Skyscrapers crumble as history fractures."),
        HistoricalEvent(2004, "Facebook is launching today, weaving the first digital social net."),
        HistoricalEvent(2007, "Steve Jobs is revealing the first iPhone today, launching the screen era."),
        HistoricalEvent(2012, "The Curiosity Rover has descended upon Mars today, searching for ancient streams."),
        HistoricalEvent(2015, "Pluto is imaged in stunning high-definition by New Horizons today."),
        HistoricalEvent(2020, "Global lockdowns are descending today. Quietness wraps the planet."),
        HistoricalEvent(2023, "Generative AI is sweeping the technological landscape today.")
    )

    private data class HistoricalEvent(val year: Int, val description: String)

    fun getStorytellingText(isNegative: Boolean, targetDate: LocalDateTime, ageYears: Long): String {
        return if (isNegative) {
            val targetYear = targetDate.year
            // Find the closest historical anchor in time
            val closest = historicalAnchors.minByOrNull { Math.abs(it.year - targetYear) }
            if (closest != null) {
                "${closest.description} You are -$ageYears years away from your first breath."
            } else {
                "Deep historic timelines. The year is $targetYear. You are -$ageYears years away from your first breath."
            }
        } else {
            // Life Eras poetry
            when {
                ageYears < 1 -> "The wonder of infancy. A brand-new code block of life."
                ageYears in 1..12 -> "The magic of childhood. The entire universe is a playground."
                ageYears in 13..17 -> "The storm of youth. Constructing the self and defining paths."
                ageYears == 18L -> "You are legally a sovereign adult today. Crucial vectors open before you."
                ageYears == 21L -> "You are fully unlocked. Deep agency can now be exerted."
                ageYears == 25L -> "On this day, your prefrontal cortex is fully fused. Peerless reason is active."
                ageYears in 30..34 -> "Eighty percent of life's decisions are set by 35. You are in your active prime."
                ageYears == 35L -> "You are officially of legal status to run for President of the United States."
                ageYears in 40..49 -> "Entering mid-life’s peak. Sifting deep wisdom through golden decades."
                ageYears in 50..59 -> "Half a century of narratives. Your actions carry a grand foundational weight."
                ageYears in 60..69 -> "Diamond age. Your view on modern timelines has transcended transient noise."
                ageYears >= 70L -> "Sovereign sage. Every single rotation from here is a precious bonus round."
                else -> "On this vector block, you are $ageYears years old—navigating your unique, active coordinate."
            }
        }
    }
}
