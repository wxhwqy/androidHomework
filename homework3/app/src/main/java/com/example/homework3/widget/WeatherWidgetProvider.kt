package com.example.homework3.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.widget.RemoteViews
import com.example.homework3.MainActivity
import com.example.homework3.R
import com.example.homework3.repository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class WeatherWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_REFRESH = "com.example.homework3.widget.REFRESH"
        private const val ACTION_OPEN_APP = "com.example.homework3.widget.OPEN_APP"
        
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, WeatherWidgetProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WeatherWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_REFRESH -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, WeatherWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.weather_widget)
        
        setupClickEvents(context, views)
        
        showLoadingState(views)
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
        
        loadWeatherData(context, appWidgetManager, appWidgetId, views)
    }

    private fun setupClickEvents(context: Context, views: RemoteViews) {
        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_temperature, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_weather_desc, openAppPendingIntent)
        
        val refreshIntent = Intent(context, WeatherWidgetProvider::class.java)
        refreshIntent.action = ACTION_REFRESH
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, 0, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)
    }

    private fun showLoadingState(views: RemoteViews) {
        views.setTextViewText(R.id.widget_city_name, "当前位置")
        views.setTextViewText(R.id.widget_temperature, "--°")
        views.setTextViewText(R.id.widget_temp_range, "--° / --°")
        views.setTextViewText(R.id.widget_weather_desc, "加载中...")
        views.setTextViewText(R.id.widget_feels_like, "体感 --°")
        views.setTextViewText(R.id.widget_humidity, "湿度 --%")
        //views.setTextViewText(R.id.widget_wind, "-- --km/h")
        
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        //views.setTextViewText(R.id.widget_update_time, currentTime)
    }

    private fun showErrorState(views: RemoteViews, error: String) {
        views.setTextViewText(R.id.widget_city_name, "当前位置")
        views.setTextViewText(R.id.widget_temperature, "--°")
        views.setTextViewText(R.id.widget_temp_range, "--° / --°")
        views.setTextViewText(R.id.widget_weather_desc, "加载失败")
        views.setTextViewText(R.id.widget_feels_like, error)
        views.setTextViewText(R.id.widget_humidity, "湿度 --%")
        //views.setTextViewText(R.id.widget_wind, "-- --km/h")
        
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        //views.setTextViewText(R.id.widget_update_time, currentTime)
    }

    private fun loadWeatherData(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, views: RemoteViews) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = WeatherRepository(context)
                
                val location = getCurrentLocation(context)
                if (location != null) {
                    val result = repository.getWeatherInfoByCoord(location.first, location.second)
                    
                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            val weatherInfo = result.getOrNull()
                            if (weatherInfo != null) {
                                updateWeatherUI(views, weatherInfo)
                            } else {
                                showErrorState(views, "数据为空")
                            }
                        } else {
                            val error = result.exceptionOrNull()?.message ?: "未知错误"
                            showErrorState(views, error)
                        }
                        
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } else {
                    val currentLocationCity = repository.getCurrentLocationCity()
                    if (currentLocationCity != null) {
                        val result = repository.getWeatherInfo(currentLocationCity.location)
                        
                        withContext(Dispatchers.Main) {
                            if (result.isSuccess) {
                                val weatherInfo = result.getOrNull()
                                if (weatherInfo != null) {
                                    updateWeatherUI(views, weatherInfo, currentLocationCity.name)
                                } else {
                                    showErrorState(views, "数据为空")
                                }
                            } else {
                                val error = result.exceptionOrNull()?.message ?: "未知错误"
                                showErrorState(views, error)
                            }
                            
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showErrorState(views, "无位置信息")
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorState(views, "加载失败: ${e.message}")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }

    private fun updateWeatherUI(views: RemoteViews, weatherInfo: com.example.homework3.data.CityWeatherInfo, cityName: String? = null) {
        views.setTextViewText(R.id.widget_city_name, cityName ?: "当前位置")
        
        weatherInfo.currentWeather?.let { current ->
            views.setTextViewText(R.id.widget_temperature, "${current.temp}°")
            views.setTextViewText(R.id.widget_weather_desc, current.text)
            views.setTextViewText(R.id.widget_feels_like, "体感 ${current.feelsLike}°")
            views.setTextViewText(R.id.widget_humidity, "湿度 ${current.humidity}%")
            //views.setTextViewText(R.id.widget_wind, "${current.windDir} ${current.windSpeed}km/h")
        }
        
        if (weatherInfo.dailyWeather.isNotEmpty()) {
            val today = weatherInfo.dailyWeather[0]
            views.setTextViewText(R.id.widget_temp_range, "${today.tempMin}° / ${today.tempMax}°")
        }
        
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        //views.setTextViewText(R.id.widget_update_time, currentTime)
    }

    private fun getCurrentLocation(context: Context): Pair<Double, Double>? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return null
            }
            
            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            
            if (lastKnownLocation != null) {
                Pair(lastKnownLocation.latitude, lastKnownLocation.longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

} 