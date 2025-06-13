package com.example.homework3.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    
    @Query("SELECT * FROM cities ORDER BY orderIndex ASC")
    fun getAllCities(): Flow<List<CityEntity>>
    
    @Query("SELECT * FROM cities WHERE id = :cityId")
    suspend fun getCityById(cityId: Long): CityEntity?
    
    @Query("SELECT * FROM cities WHERE isCurrentLocation = 1 LIMIT 1")
    suspend fun getCurrentLocationCity(): CityEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: CityEntity): Long
    
    @Update
    suspend fun updateCity(city: CityEntity)
    
    @Delete
    suspend fun deleteCity(city: CityEntity)
    
    @Query("DELETE FROM cities WHERE id = :cityId")
    suspend fun deleteCityById(cityId: Long)
    
    @Query("UPDATE cities SET orderIndex = :newIndex WHERE id = :cityId")
    suspend fun updateCityOrder(cityId: Long, newIndex: Int)
    
    @Query("SELECT * FROM weather_cache WHERE location = :location")
    suspend fun getWeatherCache(location: String): WeatherCacheEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherCache(cache: WeatherCacheEntity)
    
    @Query("DELETE FROM weather_cache WHERE location = :location")
    suspend fun deleteWeatherCache(location: String)
    
    @Query("DELETE FROM weather_cache WHERE cacheTime < :expireTime")
    suspend fun deleteExpiredCache(expireTime: Long)
    
    @Query("SELECT * FROM weather_cache WHERE location = :location AND cacheTime > :minTime")
    suspend fun getValidWeatherCache(location: String, minTime: Long): WeatherCacheEntity?
    
    @Query("SELECT COUNT(*) FROM weather_cache")
    suspend fun getCacheCount(): Int
    
    @Query("DELETE FROM weather_cache")
    suspend fun clearAllCache()
    
    @Query("SELECT COUNT(*) FROM cities")
    suspend fun getCityCount(): Int
} 