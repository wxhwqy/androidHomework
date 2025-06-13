package com.example.homework3

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.homework3.databinding.ActivityApiTestBinding
import com.example.homework3.network.WeatherApiService
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ApiTestActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityApiTestBinding
    private lateinit var apiService: WeatherApiService
    private val gson = Gson()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApiTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.title = "API测试"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        apiService = WeatherApiService.getInstance()
        
        setupClickListeners()
        
        // 默认测试城市
        binding.editTextLocation.setText("beijing")
    }
    
    private fun setupClickListeners() {
        // 测试当前天气
        binding.buttonTestCurrentWeather.setOnClickListener {
            val location = binding.editTextLocation.text.toString().trim()
            if (location.isNotEmpty()) {
                testCurrentWeather(location)
            } else {
                Toast.makeText(this, "请输入城市名称或坐标", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 测试天气预报
        binding.buttonTestForecast.setOnClickListener {
            val location = binding.editTextLocation.text.toString().trim()
            if (location.isNotEmpty()) {
                testWeatherForecast(location)
            } else {
                Toast.makeText(this, "请输入城市名称或坐标", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 测试城市搜索
        binding.buttonTestCitySearch.setOnClickListener {
            val query = binding.editTextLocation.text.toString().trim()
            if (query.isNotEmpty()) {
                testCitySearch(query)
            } else {
                Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 测试坐标天气
        binding.buttonTestCoordWeather.setOnClickListener {
            testCoordinateWeather()
        }
        
        // 清空结果
        binding.buttonClear.setOnClickListener {
            binding.textViewResult.text = ""
        }
    }
    
    private fun testCurrentWeather(location: String) {
        showLoading("正在获取当前天气...")
        
        CoroutineScope(Dispatchers.IO).launch {
            apiService.getCurrentWeather(location) { response, error ->
                CoroutineScope(Dispatchers.Main).launch {
                    if (error != null) {
                        showResult("当前天气API错误:\n$error")
                    } else if (response != null) {
                        val jsonResult = gson.toJson(response)
                        showResult("当前天气API响应:\n$jsonResult")
                    } else {
                        showResult("当前天气API返回为空")
                    }
                }
            }
        }
    }
    
    private fun testWeatherForecast(location: String) {
        showLoading("正在获取天气预报...")
        
        CoroutineScope(Dispatchers.IO).launch {
            apiService.getWeatherForecast(location) { response, error ->
                CoroutineScope(Dispatchers.Main).launch {
                    if (error != null) {
                        showResult("天气预报API错误:\n$error")
                    } else if (response != null) {
                        val jsonResult = gson.toJson(response)
                        showResult("天气预报API响应:\n$jsonResult")
                    } else {
                        showResult("天气预报API返回为空")
                    }
                }
            }
        }
    }
    
    private fun testCitySearch(query: String) {
        showLoading("正在搜索城市...")
        
        CoroutineScope(Dispatchers.IO).launch {
            apiService.searchCity(query) { response, error ->
                CoroutineScope(Dispatchers.Main).launch {
                    if (error != null) {
                        showResult("城市搜索API错误:\n$error")
                    } else if (response != null) {
                        val jsonResult = gson.toJson(response)
                        showResult("城市搜索API响应:\n$jsonResult")
                    } else {
                        showResult("城市搜索API返回为空")
                    }
                }
            }
        }
    }
    
    private fun testCoordinateWeather() {
        showLoading("正在获取坐标天气...")
        
        // 使用北京的坐标作为示例
        val lat = 39.9042
        val lon = 116.4074
        
        CoroutineScope(Dispatchers.IO).launch {
            apiService.getCurrentWeatherByCoord(lat, lon) { response, error ->
                CoroutineScope(Dispatchers.Main).launch {
                    if (error != null) {
                        showResult("坐标天气API错误:\n$error")
                    } else if (response != null) {
                        val jsonResult = gson.toJson(response)
                        showResult("坐标天气API响应 (北京 $lat, $lon):\n$jsonResult")
                    } else {
                        showResult("坐标天气API返回为空")
                    }
                }
            }
        }
    }
    
    private fun showLoading(message: String) {
        binding.textViewResult.text = message
    }
    
    private fun showResult(result: String) {
        binding.textViewResult.text = result
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
} 