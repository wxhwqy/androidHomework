package com.example.homework3.network

import com.example.homework3.data.WeatherResponse
import com.google.gson.Gson
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

class WeatherApiService {
    companion object {
        private const val BASE_URL = "https://kq3h2pgah5.re.qweatherapi.com"
        private const val API_KEY = "82dd21fd4260438da4978fd2b761bd59"
        
        @Volatile
        private var INSTANCE: WeatherApiService? = null
        
        fun getInstance(): WeatherApiService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WeatherApiService().also { INSTANCE = it }
            }
        }
    }
    
    private val gson = Gson()
    private val client: OkHttpClient
    
    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    
    fun getCurrentWeather(location: String, callback: (WeatherResponse?, String?) -> Unit) {
        val url = "$BASE_URL/v7/weather/now?location=$location"
        makeRequest(url, callback)
    }
    
    fun getWeatherForecast(location: String, callback: (WeatherResponse?, String?) -> Unit) {
        val url = "$BASE_URL/v7/weather/7d?location=$location"
        makeRequest(url, callback)
    }
    
    fun getCurrentWeatherByCoord(lat: Double, lon: Double, callback: (WeatherResponse?, String?) -> Unit) {
        val location = "$lon,$lat"
        getCurrentWeather(location, callback)
    }
    
    fun getWeatherForecastByCoord(lat: Double, lon: Double, callback: (WeatherResponse?, String?) -> Unit) {
        val location = "$lon,$lat"
        getWeatherForecast(location, callback)
    }
    
    fun searchCity(query: String, callback: (CitySearchResponse?, String?) -> Unit) {
        val url = "$BASE_URL/geo/v2/city/lookup?location=$query"
        
        val request = Request.Builder()
            .url(url)
            .addHeader("X-QW-Api-Key", API_KEY)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, "网络请求失败: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val responseBody = it.body?.string()
                        if (responseBody != null) {
                            try {
                                val cityResponse = gson.fromJson(responseBody, CitySearchResponse::class.java)
                                callback(cityResponse, null)
                            } catch (e: Exception) {
                                callback(null, "解析城市数据失败: ${e.message}")
                            }
                        } else {
                            callback(null, "响应体为空")
                        }
                    } else {
                        callback(null, "请求失败: ${it.code}")
                    }
                }
            }
        })
    }
    
    private fun makeRequest(url: String, callback: (WeatherResponse?, String?) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .addHeader("X-QW-Api-Key", API_KEY)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, "网络请求失败: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val responseBody = it.body?.string()
                        if (responseBody != null) {
                            try {
                                val weatherResponse = gson.fromJson(responseBody, WeatherResponse::class.java)
                                callback(weatherResponse, null)
                            } catch (e: Exception) {
                                callback(null, "解析天气数据失败: ${e.message}")
                            }
                        } else {
                            callback(null, "响应体为空")
                        }
                    } else {
                        callback(null, "请求失败: ${it.code}")
                    }
                }
            }
        })
    }
}

data class CitySearchResponse(
    val code: String,
    val location: List<CityLocation>?
)

data class CityLocation(
    val name: String,
    val id: String,
    val lat: String,
    val lon: String,
    val adm2: String, // 地级市
    val adm1: String, // 省份
    val country: String, // 国家
    val tz: String, // 时区
    val utcOffset: String, // UTC偏移
    val isDst: String, // 是否夏令时
    val type: String, // 地点类型
    val rank: String, // 地点等级
    val fxLink: String // 预报链接
) 