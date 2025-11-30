package com.fatih.pomodoroapp1.domain.model

data class TimerState(
    val timeRemainingSeconds: Int,
    val totalTimeSeconds: Int,
    val isPaused: Boolean = true,
    val currentPomodoro: Int = 1,
    val totalPomodoros: Int = 4,
    val phase: TimerPhase = TimerPhase.POMODORO
) {
    val progress: Float
        get() = if (totalTimeSeconds > 0) {
            timeRemainingSeconds.toFloat() / totalTimeSeconds.toFloat()
        } else 0f

    val minutes: Int
        get() = timeRemainingSeconds / 60

    val seconds: Int
        get() = timeRemainingSeconds % 60
}

enum class TimerPhase {
    POMODORO,
    SHORT_BREAK,
    LONG_BREAK
}