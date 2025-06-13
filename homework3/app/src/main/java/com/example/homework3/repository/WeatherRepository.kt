package com.example.homework3.repository

import android.content.Context
import com.example.homework3.data.*
import com.example.homework3.database.*
import com.example.homework3.network.WeatherApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class WeatherRepository(context: Context) {
    
    private val database = WeatherDatabase.getDatabase(context)
    private val weatherDao = database.weatherDao()
    private val apiService = WeatherApiService.getInstance()
    
    companion object {
        private const val CACHE_EXPIRY_TIME = 60 * 60 * 1000L 
    }
    
    fun getAllCities(): Flow<List<CityInfo>> {
        return weatherDao.getAllCities().map { entities ->
            entities.map { entity ->
                CityInfo(
                    id = entity.id,
                    name = entity.name,
                    location = entity.location,
                    isCurrentLocation = entity.isCurrentLocation,
                    latitude = entity.latitude,
                    longitude = entity.longitude
                )
            }
        }
    }
    
    suspend fun addCity(city: CityInfo): Long = withContext(Dispatchers.IO) {
        val cityEntity = CityEntity(
            name = city.name,
            location = city.location,
            isCurrentLocation = city.isCurrentLocation,
            latitude = city.latitude,
            longitude = city.longitude,
            orderIndex = 0 
        )
        weatherDao.insertCity(cityEntity)
    }
    
    suspend fun getCurrentLocationCity(): CityInfo? = withContext(Dispatchers.IO) {
        val cityEntity = weatherDao.getCurrentLocationCity()
        return@withContext cityEntity?.let {
            CityInfo(
                id = it.id,
                name = it.name,
                location = it.location,
                isCurrentLocation = it.isCurrentLocation,
                latitude = it.latitude,
                longitude = it.longitude
            )
        }
    }
    
    suspend fun updateOrAddCurrentLocationCity(cityInfo: CityInfo): Long = withContext(Dispatchers.IO) {
        val existingCity = weatherDao.getCurrentLocationCity()
        
        if (existingCity != null) {
            val updatedCity = existingCity.copy(
                location = cityInfo.location,
                latitude = cityInfo.latitude,
                longitude = cityInfo.longitude
            )
            weatherDao.updateCity(updatedCity)
            existingCity.id
        } else {
            addCity(cityInfo)
        }
    }
    
    suspend fun hasCities(): Boolean = withContext(Dispatchers.IO) {
        try {
            weatherDao.getCityCount() > 0
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getWeatherInfo(location: String): Result<CityWeatherInfo> = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val validCacheTime = currentTime - CACHE_EXPIRY_TIME
            val cachedWeather = weatherDao.getValidWeatherCache(location, validCacheTime)
            
            if (cachedWeather != null) {
                val cityInfo = CityInfo(
                    name = "缓存城市",
                    location = location
                )
                
                val weatherInfo = CityWeatherInfo(
                    city = cityInfo,
                    currentWeather = cachedWeather.currentWeather,
                    dailyWeather = cachedWeather.dailyWeather,
                    updateTime = cachedWeather.updateTime
                )
                
                return@withContext Result.success(weatherInfo)
            }
            
            fetchWeatherFromNetwork(location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getWeatherInfoByCoord(lat: Double, lon: Double): Result<CityWeatherInfo> = withContext(Dispatchers.IO) {
        val location = "$lon,$lat"
        fetchWeatherFromNetwork(location)
    }
    
    private suspend fun fetchWeatherFromNetwork(location: String): Result<CityWeatherInfo> = withContext(Dispatchers.IO) {
        try {
            val currentWeatherResult = suspendCancellableCoroutine<Pair<CurrentWeather?, String?>> { continuation ->
                apiService.getCurrentWeather(location) { response, error ->
                    if (error != null) {
                        continuation.resume(Pair(null, error))
                    } else {
                        continuation.resume(Pair(response?.now, null))
                    }
                }
            }
            
            val currentWeather = currentWeatherResult.first
            val currentError = currentWeatherResult.second
            
            if (currentError != null) {
                return@withContext Result.failure(Exception(currentError))
            }
            
            val forecastResult = suspendCancellableCoroutine<Pair<List<DailyWeather>, String?>> { continuation ->
                apiService.getWeatherForecast(location) { response, error ->
                    if (error != null) {
                        continuation.resume(Pair(emptyList<DailyWeather>(), error))
                    } else {
                        continuation.resume(Pair(response?.daily ?: emptyList<DailyWeather>(), null))
                    }
                }
            }
            
            val dailyWeather = forecastResult.first
            val forecastError = forecastResult.second
            
            if (forecastError != null && dailyWeather.isEmpty()) {
                return@withContext Result.failure(Exception(forecastError))
            }
            
            val updateTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.getDefault())
                .format(java.util.Date())
            
            val cacheEntity = WeatherCacheEntity(
                location = location,
                currentWeather = currentWeather,
                dailyWeather = dailyWeather,
                updateTime = updateTime,
                cacheTime = System.currentTimeMillis()
            )
            weatherDao.insertWeatherCache(cacheEntity)
            
            val cityInfo = CityInfo(
                name = "未知城市",
                location = location
            )
            
            val weatherInfo = CityWeatherInfo(
                city = cityInfo,
                currentWeather = currentWeather,
                dailyWeather = dailyWeather,
                updateTime = updateTime
            )
            
            Result.success(weatherInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun cleanExpiredCache() = withContext(Dispatchers.IO) {
        val expireTime = System.currentTimeMillis() - CACHE_EXPIRY_TIME
        weatherDao.deleteExpiredCache(expireTime)
    }
    
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        weatherDao.clearAllCache()
    }
    
    suspend fun searchCities(cityName: String): Result<List<CityInfo>> = withContext(Dispatchers.IO) {
        try {
            val searchResult = suspendCancellableCoroutine<Pair<com.example.homework3.network.CitySearchResponse?, String?>> { continuation ->
                apiService.searchCity(cityName) { response, error ->
                    continuation.resume(Pair(response, error))
                }
            }
            
            val cityResponse = searchResult.first
            val error = searchResult.second
            
            if (error != null) {
                return@withContext Result.failure(Exception(error))
            }
            
            if (cityResponse != null && !cityResponse.location.isNullOrEmpty()) {
                val cities = cityResponse.location.map { cityLocation ->
                    CityInfo(
                        name = "${cityLocation.name}, ${cityLocation.adm1}, ${cityLocation.country}",
                        location = cityLocation.id, 
                        isCurrentLocation = false,
                        latitude = cityLocation.lat.toDoubleOrNull() ?: 0.0,
                        longitude = cityLocation.lon.toDoubleOrNull() ?: 0.0
                    )
                }
                
                Result.success(cities)
            } else {
                Result.failure(Exception("未找到城市: $cityName"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun searchAndAddCity(cityName: String): Result<CityInfo> = withContext(Dispatchers.IO) {
        try {
            val searchResult = searchCities(cityName)
            
            if (searchResult.isSuccess) {
                val cities = searchResult.getOrNull()
                if (!cities.isNullOrEmpty()) {
                    val cityInfo = cities[0] 
                    val cityId = addCity(cityInfo)
                    Result.success(cityInfo.copy(id = cityId))
                } else {
                    Result.failure(Exception("未找到城市: $cityName"))
                }
            } else {
                searchResult as Result<CityInfo>
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 