package com.rehealth.genie.ring.device

import com.manridy.sdk_mrd2019.bean.common.DrinkReminderBean
import com.manridy.sdk_mrd2019.bean.send.DayRepeatFlag
import com.manridy.sdk_mrd2019.bean.send.MrdEventClock
import com.manridy.sdk_mrd2019.bean.send.MrdHeartBloodAlert
import com.manridy.sdk_mrd2019.bean.send.MrdNotDisturb
import com.manridy.sdk_mrd2019.bean.send.MrdSedentary
import com.manridy.sdk_mrd2019.bean.send.MrdUserInfo
import com.manridy.sdk_mrd2019.bean.send.MrdWeather

/**
 * 设备设置数据模型
 * 对应原厂MainActivity.java中的各种设置Bean
 */

// 闹钟设置
data class AlarmClock(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val label: String,
    val repeatDays: List<DayRepeatFlag> = DayRepeatFlag.values().toList()
) {
    fun toMrdEventClock(): MrdEventClock =
        MrdEventClock(enabled, hour, minute, label, repeatDays.toTypedArray())
}

// 久坐提醒
data class SedentaryReminder(
    val enabled: Boolean,
    val intervalMinutes: Int = 30,
    val startTime: String = "09:00",
    val endTime: String = "19:00",
    val lunchStart: String = "12:30",
    val lunchEnd: String = "14:00",
    val noDisturbLunch: Boolean = true
) {
    fun toMrdSedentary(): MrdSedentary =
        MrdSedentary(enabled, noDisturbLunch, startTime, endTime, lunchStart, lunchEnd, intervalMinutes)
}

// 喝水提醒
data class DrinkReminder(
    val enabled: Boolean,
    val intervalMinutes: Int = 20,
    val startTime: String = "09:00",
    val endTime: String = "20:00",
    val lunchNoDisturb: Boolean = true,
    val lunchStart: String = "12:30",
    val lunchEnd: String = "14:00"
) {
    fun toDrinkReminderBean(): DrinkReminderBean =
        DrinkReminderBean(enabled, startTime, endTime, lunchNoDisturb, lunchStart, lunchEnd, intervalMinutes)
}

// 勿扰模式
data class DoNotDisturb(
    val enabled: Boolean,
    val startTime: String = "22:00",
    val endTime: String = "08:00"
) {
    fun toMrdNotDisturb(): MrdNotDisturb =
        MrdNotDisturb(enabled, startTime, endTime)
}

// 健康预警
data class HealthAlert(
    val hrEnabled: Boolean = false,
    val bpEnabled: Boolean = false,
    val hrHigh: Int = 120,
    val hrLow: Int = 50,
    val bpHigh: Int = 140,
    val bpLow: Int = 90
) {
    fun toMrdHeartBloodAlert(): MrdHeartBloodAlert =
        MrdHeartBloodAlert(hrEnabled, bpEnabled, hrHigh, bpHigh)
}

// 用户信息
data class UserInfo(
    val gender: Int = 1, // 0=女, 1=男
    val age: Int = 28,
    val height: Int = 170, // cm
    val weight: Int = 60,  // kg
    val walkStep: Int = 100,
    val runStep: Int = 100
) {
    fun toMrdUserInfo(): MrdUserInfo =
        MrdUserInfo().apply {
            sex = gender
            age = this@UserInfo.age
            height = this@UserInfo.height
            weight = this@UserInfo.weight
            walk = walkStep
            run = runStep
        }
}

// 运动目标
data class SportTarget(
    val stepTarget: Int = 10000,
    val calorieTarget: Int = 2000
)

// 单位设置
data class UnitSettings(
    val lengthUnit: Int = 1, // 0=英制, 1=公制
    val weightUnit: Int = 1, // 0=磅, 1=公斤
    val is24Hour: Boolean = true
)

// 天气信息
data class WeatherInfo(
    val dayIndex: Int, // 0=今天, 1=明天...
    val weatherType: Int, // 天气类型
    val highTemp: Int,
    val lowTemp: Int,
    val currentTemp: Int
) {
    fun toMrdWeather(): MrdWeather =
        MrdWeather(dayIndex, weatherType, lowTemp, highTemp)
}
