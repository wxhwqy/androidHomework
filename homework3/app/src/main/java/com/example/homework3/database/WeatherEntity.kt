package com.example.homework3.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.homework3.data.CurrentWeather
import com.example.homework3.data.DailyWeather

@Entity(tableName = "cities")
data class CityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val location: String, 
    val isCurrentLocation: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val orderIndex: Int = 0 
)

@Entity(tableName = "weather_cache")
@TypeConverters(Converters::class)
data class WeatherCacheEntity(
    @PrimaryKey
    val location: String, 
    val currentWeather: CurrentWeather?,
    val dailyWeather: List<DailyWeather>,
    val updateTime: String,
    val cacheTime: Long 
)

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromCurrentWeather(weather: CurrentWeather?): String? {
        return weather?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toCurrentWeather(weatherString: String?): CurrentWeather? {
        return weatherString?.let {
            gson.fromJson(it, CurrentWeather::class.java)
        }
    }
    
    @TypeConverter
    fun fromDailyWeatherList(dailyWeather: List<DailyWeather>): String {
        return gson.toJson(dailyWeather)
    }
    
    @TypeConverter
    fun toDailyWeatherList(dailyWeatherString: String): List<DailyWeather> {
        val listType = object : TypeToken<List<DailyWeather>>() {}.type
        return gson.fromJson(dailyWeatherString, listType)
    }
} 