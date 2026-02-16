package com.fatih.pomodoroapp1.domain.model

data class Settings(
    val pomodoroMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val pomodoroCount: Int = 4,
    val notificationsEnabled: Boolean = false
) {
    fun isValid(): Boolean {
        return pomodoroMinutes in 20..60 &&
                shortBreakMinutes in 5..30 &&
                longBreakMinutes in 10..60 &&
                pomodoroCount in 1..15
    }
}