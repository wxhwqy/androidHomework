package com.example.homework3.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.homework3.R
import com.example.homework3.data.CityWeatherInfo
import com.example.homework3.databinding.FragmentWeatherBinding
import com.example.homework3.repository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class WeatherFragment : Fragment() {
    
    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var repository: WeatherRepository
    private lateinit var forecastAdapter: WeatherForecastAdapter
    
    companion object {
        private const val ARG_LOCATION = "location"
        private const val ARG_CITY_NAME = "city_name"
        private const val ARG_IS_CURRENT_LOCATION = "is_current_location"
        
        fun newInstance(location: String, cityName: String, isCurrentLocation: Boolean = false): WeatherFragment {
            val fragment = WeatherFragment()
            val args = Bundle().apply {
                putString(ARG_LOCATION, location)
                putString(ARG_CITY_NAME, cityName)
                putBoolean(ARG_IS_CURRENT_LOCATION, isCurrentLocation)
            }
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            repository = WeatherRepository(requireContext())
            setupRecyclerView()
            
            // 延迟加载天气数据，避免初始化冲突
            view.post {
                loadWeatherData()
            }
            
            // 下拉刷新
            binding.swipeRefresh.setOnRefreshListener {
                loadWeatherData(forceRefresh = true)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Fragment初始化失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupRecyclerView() {
        forecastAdapter = WeatherForecastAdapter()
        binding.recyclerViewForecast.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = forecastAdapter
        }
    }
    
    
    private fun loadWeatherData(forceRefresh: Boolean = false) {
        val location = arguments?.getString(ARG_LOCATION)
        if (location.isNullOrBlank()) {
            Toast.makeText(requireContext(), "城市位置信息无效", Toast.LENGTH_SHORT).show()
            return
        }
        
        val cityName = arguments?.getString(ARG_CITY_NAME) ?: "未知城市"
        val isCurrentLocation = arguments?.getBoolean(ARG_IS_CURRENT_LOCATION, false) ?: false
        
        // 安全检查binding是否仍然有效
        if (_binding == null) {
            return
        }
        
        binding.swipeRefresh.isRefreshing = true
        binding.progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (forceRefresh) {
                    // 强制刷新时清除缓存
                    repository.clearAllCache()
                }
                
                val result = repository.getWeatherInfo(location)
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val weatherInfo = result.getOrNull()
                        if (weatherInfo != null) {
                            updateUI(weatherInfo)
                        }
                    } else {
                        val error = result.exceptionOrNull()
                        Toast.makeText(requireContext(), "加载天气数据失败: ${error?.message}", Toast.LENGTH_SHORT).show()
                    }
                    
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
    
    private fun updateUI(weatherInfo: CityWeatherInfo) {
        // 安全检查binding是否仍然有效
        if (_binding == null) return
        
        try {
            // 更新城市名称 - 使用传入的城市名称而不是API返回的名称
            val displayCityName = arguments?.getString(ARG_CITY_NAME) ?: weatherInfo.city.name
            binding.textCityName.text = displayCityName
        
        // 更新当前天气
        weatherInfo.currentWeather?.let { current ->
            binding.textCurrentTemp.text = "${current.temp}°"
            binding.textWeatherDescription.text = current.text
            binding.textFeelsLike.text = "体感温度 ${current.feelsLike}°"
            binding.textHumidity.text = "湿度 ${current.humidity}%"
            binding.textWindSpeed.text = "风速 ${current.windSpeed}km/h"
            binding.textWindDirection.text = current.windDir
        }
        
        // 更新今日天气范围
        if (weatherInfo.dailyWeather.isNotEmpty()) {
            val today = weatherInfo.dailyWeather[0]
            binding.textTodayRange.text = "${today.tempMin}° / ${today.tempMax}°"
        }
        
        // 更新时间
        binding.textUpdateTime.text = "更新时间: ${formatUpdateTime(weatherInfo.updateTime)}"
        
            // 更新7天预报
            forecastAdapter.updateData(weatherInfo.dailyWeather)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "更新界面失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun formatUpdateTime(updateTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            val date = inputFormat.parse(updateTime)
            if (date != null) {
                outputFormat.format(date)
            } else {
                updateTime
            }
        } catch (e: Exception) {
            updateTime
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 