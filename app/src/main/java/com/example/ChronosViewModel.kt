package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.Locale

enum class TimeFilter(val label: String) {
    YEARS("Years/Breakdown"),
    MONTHS("Months"),
    WEEKS("Weeks"),
    DAYS("Days"),
    HOURS("Hours"),
    SECONDS("Seconds")
}

data class ChronosUiState(
    // Birthdate Components (Default to 12 / 11 / 1999 00:00:00)
    val birthYear: Int = 1999,
    val birthMonth: Int = 11,
    val birthDay: Int = 12,
    val birthHour: Int = 0,
    val birthMinute: Int = 0,
    val birthSecond: Int = 0,
    
    // Target Components (Initialized to current time if isTargetLive = false)
    val targetYear: Int = 2026,
    val targetMonth: Int = 6,
    val targetDay: Int = 8,
    val targetHour: Int = 17,
    val targetMinute: Int = 14,
    val targetSecond: Int = 0,
    
    val isTargetLive: Boolean = true,
    val selectedFilter: TimeFilter = TimeFilter.YEARS,
    val displayGrandTotal: Boolean = false, // Toggle state: Grand Total vs Human Readable Breakdown
    
    // Computation outputs
    val parsedBirth: LocalDateTime? = null,
    val parsedTarget: LocalDateTime? = null,
    val vectorResult: TimeVectorResult? = null,
    val storytelling: String = "",
    
    // Symmetry alignments
    val daySymmetry: Boolean = false,
    val daySymmetryMsg: String = "",
    val chronoSync: Boolean = false,
    val chronoSyncMsg: String = ""
)

class ChronosViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = SavedDateRepository(database.savedDateDao())

    val savedDates = repository.allSavedDates.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _uiState = MutableStateFlow(ChronosUiState())
    val uiState: StateFlow<ChronosUiState> = _uiState.asStateFlow()

    init {
        // Initialize target date with current system time
        val now = LocalDateTime.now()
        _uiState.value = _uiState.value.copy(
            targetYear = now.year,
            targetMonth = now.monthValue,
            targetDay = now.dayOfMonth,
            targetHour = now.hour,
            targetMinute = now.minute,
            targetSecond = now.second
        )
        
        recomputeVector()
        
        // Dynamic millisecond tic-toc updates in background for live targets
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            while (isActive) {
                if (_uiState.value.isTargetLive) {
                    tickCurrentTime()
                }
                delay(100) // Tickers update at 10Hz; saves massive CPU resource & avoids freezing the headless emulator
            }
        }
    }

    // Birthdate Updaters
    fun updateBirthYear(year: Int) {
        val maxDays = getMaxDays(year, _uiState.value.birthMonth)
        val correctedDay = _uiState.value.birthDay.coerceIn(1, maxDays)
        _uiState.value = _uiState.value.copy(birthYear = year, birthDay = correctedDay)
        recomputeVector()
    }

    fun updateBirthMonth(month: Int) {
        val maxDays = getMaxDays(_uiState.value.birthYear, month)
        val correctedDay = _uiState.value.birthDay.coerceIn(1, maxDays)
        _uiState.value = _uiState.value.copy(birthMonth = month, birthDay = correctedDay)
        recomputeVector()
    }

    fun updateBirthDay(day: Int) {
        val maxDays = getMaxDays(_uiState.value.birthYear, _uiState.value.birthMonth)
        val correctedDay = day.coerceIn(1, maxDays)
        _uiState.value = _uiState.value.copy(birthDay = correctedDay)
        recomputeVector()
    }

    fun updateBirthHour(hour: Int) {
        _uiState.value = _uiState.value.copy(birthHour = hour.coerceIn(0, 23))
        recomputeVector()
    }

    fun updateBirthMinute(minute: Int) {
        _uiState.value = _uiState.value.copy(birthMinute = minute.coerceIn(0, 59))
        recomputeVector()
    }

    fun updateBirthSecond(second: Int) {
        _uiState.value = _uiState.value.copy(birthSecond = second.coerceIn(0, 59))
        recomputeVector()
    }

    // Target Date Updaters
    fun updateTargetYear(year: Int) {
        val maxDays = getMaxDays(year, _uiState.value.targetMonth)
        val correctedDay = _uiState.value.targetDay.coerceIn(1, maxDays)
        _uiState.value = _uiState.value.copy(targetYear = year, targetDay = correctedDay)
        recomputeVector()
    }

    fun updateTargetMonth(month: Int) {
        val maxDays = getMaxDays(_uiState.value.targetYear, month)
        val correctedDay = _uiState.value.targetDay.coerceIn(1, maxDays)
        _uiState.value = _uiState.value.copy(targetMonth = month, targetDay = correctedDay)
        recomputeVector()
    }

    fun updateTargetDay(day: Int) {
        val maxDays = getMaxDays(_uiState.value.targetYear, _uiState.value.targetMonth)
        val correctedDay = day.coerceIn(1, maxDays)
        _uiState.value = _uiState.value.copy(targetDay = correctedDay)
        recomputeVector()
    }

    fun updateTargetHour(hour: Int) {
        _uiState.value = _uiState.value.copy(targetHour = hour.coerceIn(0, 23))
        recomputeVector()
    }

    fun updateTargetMinute(minute: Int) {
        _uiState.value = _uiState.value.copy(targetMinute = minute.coerceIn(0, 59))
        recomputeVector()
    }

    fun updateTargetSecond(second: Int) {
        _uiState.value = _uiState.value.copy(targetSecond = second.coerceIn(0, 59))
        recomputeVector()
    }

    fun setTargetLive(live: Boolean) {
        _uiState.value = _uiState.value.copy(isTargetLive = live)
        if (!live) {
            val now = LocalDateTime.now()
            _uiState.value = _uiState.value.copy(
                targetYear = now.year,
                targetMonth = now.monthValue,
                targetDay = now.dayOfMonth,
                targetHour = now.hour,
                targetMinute = now.minute,
                targetSecond = now.second
            )
        }
        recomputeVector()
    }

    fun onFilterSelected(filter: TimeFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun toggleDisplayMode() {
        val current = _uiState.value.displayGrandTotal
        _uiState.value = _uiState.value.copy(displayGrandTotal = !current)
    }

    private fun getMaxDays(year: Int, month: Int): Int {
        return when (month.coerceIn(1, 12)) {
            4, 6, 9, 11 -> 30
            2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
            else -> 31
        }
    }

    private fun tickCurrentTime() {
        val state = _uiState.value
        val birth = state.parsedBirth ?: return
        val target = LocalDateTime.now()
        
        val result = DateTimelineEngine.computeTimeVector(birth, target)
        // High-precision Milli ticks
        val updatedResult = result.copy(
            totalMillis = if (result.isNegative) result.totalMillis else java.time.temporal.ChronoUnit.MILLIS.between(birth, target)
        )
        
        val story = DateTimelineEngine.getStorytellingText(
            isNegative = updatedResult.isNegative,
            targetDate = target,
            ageYears = updatedResult.breakdownYears
        )

        val (daySym, daySymMsg) = evaluateDaySymmetry(birth, target)
        val (chronoSync, chronoSyncMsg) = evaluateChronoSync(birth, target)

        _uiState.value = state.copy(
            parsedTarget = target,
            vectorResult = updatedResult,
            storytelling = story,
            daySymmetry = daySym,
            daySymmetryMsg = daySymMsg,
            chronoSync = chronoSync,
            chronoSyncMsg = chronoSyncMsg
        )
    }

    private fun recomputeVector() {
        val state = _uiState.value
        val birthDate = try {
            LocalDateTime.of(state.birthYear, state.birthMonth, state.birthDay, state.birthHour, state.birthMinute, state.birthSecond)
        } catch (e: Exception) {
            null
        }

        val targetDate = if (state.isTargetLive) {
            LocalDateTime.now()
        } else {
            try {
                LocalDateTime.of(state.targetYear, state.targetMonth, state.targetDay, state.targetHour, state.targetMinute, state.targetSecond)
            } catch (e: Exception) {
                null
            }
        }

        if (birthDate == null || targetDate == null) {
            _uiState.value = state.copy(
                parsedBirth = birthDate,
                parsedTarget = targetDate,
                vectorResult = null,
                storytelling = "Awaiting a valid coordinate system selection..."
            )
            return
        }

        val result = DateTimelineEngine.computeTimeVector(birthDate, targetDate)
        val story = DateTimelineEngine.getStorytellingText(
            isNegative = result.isNegative,
            targetDate = targetDate,
            ageYears = result.breakdownYears
        )

        val (daySym, daySymMsg) = evaluateDaySymmetry(birthDate, targetDate)
        val (chronoSyncVal, chronoSyncMsgVal) = evaluateChronoSync(birthDate, targetDate)

        _uiState.value = state.copy(
            parsedBirth = birthDate,
            parsedTarget = targetDate,
            vectorResult = result,
            storytelling = story,
            daySymmetry = daySym,
            daySymmetryMsg = daySymMsg,
            chronoSync = chronoSyncVal,
            chronoSyncMsg = chronoSyncMsgVal
        )
    }

    private fun evaluateDaySymmetry(birth: LocalDateTime, target: LocalDateTime): Pair<Boolean, String> {
        val match = birth.dayOfWeek == target.dayOfWeek
        return if (match) {
            val dayName = birth.dayOfWeek.name.lowercase(Locale.US).replaceFirstChar { it.uppercaseChar() }
            true to "✨ $dayName Symmetry — Both times fall on a $dayName. The calendar has aligned perfectly."
        } else {
            false to ""
        }
    }

    private fun evaluateChronoSync(birth: LocalDateTime, target: LocalDateTime): Pair<Boolean, String> {
        val match = birth.hour == target.hour && birth.minute == target.minute
        return if (match) {
            val hourFormatted = String.format(Locale.US, "%02d", birth.hour)
            val minFormatted = String.format(Locale.US, "%02d", birth.minute)
            true to "⏳ Chrono Sync — Both coordinates share the exact timestamp of $hourFormatted:$minFormatted. A temporal match!"
        } else {
            false to ""
        }
    }

    fun saveDate(title: String, year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int) {
        viewModelScope.launch {
            repository.insert(
                SavedDate(
                    title = title,
                    year = year,
                    month = month,
                    day = day,
                    hour = hour,
                    minute = minute,
                    second = second
                )
            )
        }
    }

    fun deleteSavedDate(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun applySavedDateAsBirth(savedDate: SavedDate) {
        _uiState.value = _uiState.value.copy(
            birthYear = savedDate.year,
            birthMonth = savedDate.month,
            birthDay = savedDate.day,
            birthHour = savedDate.hour,
            birthMinute = savedDate.minute,
            birthSecond = savedDate.second
        )
        recomputeVector()
    }

    fun applySavedDateAsTarget(savedDate: SavedDate) {
        _uiState.value = _uiState.value.copy(
            isTargetLive = false,
            targetYear = savedDate.year,
            targetMonth = savedDate.month,
            targetDay = savedDate.day,
            targetHour = savedDate.hour,
            targetMinute = savedDate.minute,
            targetSecond = savedDate.second
        )
        recomputeVector()
    }
}
