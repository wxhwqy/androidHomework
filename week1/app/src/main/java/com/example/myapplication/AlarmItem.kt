package com.example.myapplication

import java.util.UUID

data class AlarmItem(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val label: String = "闹钟",
    val repeatType: RepeatType = RepeatType.ONCE
)

enum class RepeatType(val displayName: String) {
    ONCE("只响一次"),
    DAILY("每天"),
    WEEKDAYS("工作日"),
    WEEKENDS("周末")
}

fun List<AlarmItem>.sortedByTime(): List<AlarmItem> {
    return this.sortedWith(compareBy<AlarmItem> { it.hour }.thenBy { it.minute })
} 