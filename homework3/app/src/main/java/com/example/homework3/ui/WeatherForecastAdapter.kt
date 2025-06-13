package com.example.homework3.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.homework3.R
import com.example.homework3.data.DailyWeather
import java.text.SimpleDateFormat
import java.util.*

class WeatherForecastAdapter : RecyclerView.Adapter<WeatherForecastAdapter.ForecastViewHolder>() {
    
    private var forecastList: List<DailyWeather> = emptyList()
    
    fun updateData(newData: List<DailyWeather>) {
        forecastList = newData
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weather_forecast, parent, false)
        return ForecastViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val forecast = forecastList[position]
        holder.bind(forecast, position)
    }
    
    override fun getItemCount(): Int = forecastList.size
    
    class ForecastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textDate: TextView = itemView.findViewById(R.id.text_date)
        private val textDayWeather: TextView = itemView.findViewById(R.id.text_day_weather)
        private val textNightWeather: TextView = itemView.findViewById(R.id.text_night_weather)
        private val textMaxTemp: TextView = itemView.findViewById(R.id.text_max_temp)
        private val textMinTemp: TextView = itemView.findViewById(R.id.text_min_temp)
        private val textHumidity: TextView = itemView.findViewById(R.id.text_humidity)
        private val textWind: TextView = itemView.findViewById(R.id.text_wind)
        
        fun bind(forecast: DailyWeather, position: Int) {
            // 格式化日期
            textDate.text = formatDate(forecast.fxDate, position)
            
            // 天气描述
            textDayWeather.text = "白天: ${forecast.textDay}"
            textNightWeather.text = "夜间: ${forecast.textNight}"
            
            // 温度
            textMaxTemp.text = "${forecast.tempMax}°"
            textMinTemp.text = "${forecast.tempMin}°"
            
            // 湿度
            textHumidity.text = "湿度: ${forecast.humidity}%"
            
            // 风况
            textWind.text = "${forecast.windDirDay} ${forecast.windScaleDay}级"
        }
        
        private fun formatDate(dateString: String, position: Int): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                
                when (position) {
                    0 -> "今天"
                    1 -> "明天"
                    2 -> "后天"
                    else -> {
                        val outputFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                        if (date != null) {
                            outputFormat.format(date)
                        } else {
                            dateString
                        }
                    }
                }
            } catch (e: Exception) {
                dateString
            }
        }
    }
} 