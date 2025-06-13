package com.example.homework3.data

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val code: String,
    val updateTime: String,
    val fxLink: String,
    val now: CurrentWeather?,
    val daily: List<DailyWeather>?
)

data class CurrentWeather(
    val obsTime: String,
    val temp: String,
    val feelsLike: String,
    val icon: String,
    val text: String,
    val wind360: String,
    val windDir: String,
    val windScale: String,
    val windSpeed: String,
    val humidity: String,
    val precip: String,
    val pressure: String,
    val vis: String,
    val cloud: String,
    val dew: String
)

data class DailyWeather(
    val fxDate: String,
    val sunrise: String,
    val sunset: String,
    val moonrise: String,
    val moonset: String,
    val moonPhase: String,
    val moonPhaseIcon: String,
    val tempMax: String,
    val tempMin: String,
    val iconDay: String,
    val textDay: String,
    val iconNight: String,
    val textNight: String,
    val wind360Day: String,
    val windDirDay: String,
    val windScaleDay: String,
    val windSpeedDay: String,
    val wind360Night: String,
    val windDirNight: String,
    val windScaleNight: String,
    val windSpeedNight: String,
    val humidity: String,
    val precip: String,
    val pressure: String,
    val vis: String,
    val cloud: String,
    val uvIndex: String
)

data class CityInfo(
    val id: Long = 0,
    val name: String,
    val location: String, 
    val isCurrentLocation: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class CityWeatherInfo(
    val city: CityInfo,
    val currentWeather: CurrentWeather?,
    val dailyWeather: List<DailyWeather>,
    val updateTime: String
) 