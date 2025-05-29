package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class AlarmDataManager(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("alarm_data", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveAlarms(alarms: List<AlarmItem>) {
        val json = gson.toJson(alarms)
        sharedPreferences.edit().putString("alarms", json).apply()
    }
    
    fun getAlarms(): List<AlarmItem> {
        val json = sharedPreferences.getString("alarms", null) ?: return emptyList()
        val type = object : TypeToken<List<AlarmItem>>() {}.type
        return gson.fromJson(json, type)
    }
    
    fun getNextEnabledAlarm(): AlarmItem? {
        val alarms = getAlarms().filter { it.isEnabled }
        if (alarms.isEmpty()) return null
        
        val currentTime = Calendar.getInstance()
        var nextAlarm: AlarmItem? = null
        var minTimeDiff = Long.MAX_VALUE
        
        for (alarm in alarms) {
            val alarmTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                if (timeInMillis <= currentTime.timeInMillis) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            
            val timeDiff = alarmTime.timeInMillis - currentTime.timeInMillis
            if (timeDiff > 0 && timeDiff < minTimeDiff) {
                minTimeDiff = timeDiff
                nextAlarm = alarm
            }
        }
        
        return nextAlarm
    }
    
    fun getTimeUntilNextAlarm(): String {
        val nextAlarm = getNextEnabledAlarm() ?: return "无闹钟"
        
        val currentTime = Calendar.getInstance()
        val alarmTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, nextAlarm.hour)
            set(Calendar.MINUTE, nextAlarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            if (timeInMillis <= currentTime.timeInMillis) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        val timeDiff = alarmTime.timeInMillis - currentTime.timeInMillis
        
        if (timeDiff <= 0) {
            return "无闹钟"
        }
        
        val hours = timeDiff / (1000 * 60 * 60)
        val minutes = (timeDiff % (1000 * 60 * 60)) / (1000 * 60)
        
        return when {
            hours > 0 -> "${hours}小时${minutes}分钟后响铃"
            minutes > 0 -> "${minutes}分钟后响铃"
            else -> "即将响铃"
        }
    }
} 