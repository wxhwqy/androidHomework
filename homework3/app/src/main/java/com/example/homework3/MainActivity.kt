package com.example.homework3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.homework3.data.CityInfo
import com.example.homework3.databinding.ActivityMainBinding
import com.example.homework3.repository.WeatherRepository
import com.example.homework3.ui.WeatherPagerAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import com.example.homework3.widget.WeatherWidgetProvider

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: WeatherRepository
    private lateinit var pagerAdapter: WeatherPagerAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var hasRequestedPermission = false
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        try {
            repository = WeatherRepository(this)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            
            setupViewPager()
            setupButtons()
            
            // 简化的初始化逻辑 - 延迟执行避免初始化冲突
            binding.viewPager.post {
                startObservingCities()
                initializeDefaultData()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
        
        // 设置页面切换监听器
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePageIndicator(position)
            }
        })

    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_city -> {
                showAddCityDialog()
                true
            }
            R.id.action_api_test -> {
                startActivity(Intent(this, ApiTestActivity::class.java))
                true
            }
            R.id.action_clear_cache -> {
                clearDatabaseCache()
                true
            }
            R.id.action_update_location -> {
                updateCurrentLocation()
                true
            }
            R.id.action_refresh -> {
                refreshCurrentPage()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupViewPager() {
        pagerAdapter = WeatherPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
    }
    
    private fun setupButtons() {
        // 添加城市按钮
        binding.buttonAddCity.setOnClickListener {
            showAddCityDialog()
        }
        
        // 刷新按钮
        binding.buttonRefresh.setOnClickListener {
            refreshCurrentPage()
        }
        
        // 菜单按钮
        binding.buttonMenu.setOnClickListener {
            showMenuPopup()
        }
    }
    
    private fun startObservingCities() {
        lifecycleScope.launch {
            try {
                repository.getAllCities().collect { cities ->
                    // 确保在主线程中更新UI
                    runOnUiThread {
                        try {
                            pagerAdapter.updateCities(cities)
                            if (cities.isNotEmpty()) {
                                updatePageIndicator(binding.viewPager.currentItem)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "更新UI失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "观察城市列表失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun initializeDefaultData() {
        lifecycleScope.launch {
            try {
                // 检查是否已有城市数据
                val hasCities = repository.hasCities()
                if (!hasCities) {
                    // 只有在没有任何城市时才请求位置权限或添加默认城市
                    requestLocationPermission()
                }
            } catch (e: Exception) {
                // 如果检查失败，仍然尝试请求位置权限
                requestLocationPermission()
            }
        }
    }
    
    private fun requestLocationPermission() {
        hasRequestedPermission = true
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getCurrentLocation()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation()
                } else {
                    Toast.makeText(this, "需要位置权限来获取当前位置的天气", Toast.LENGTH_LONG).show()
                    // 检查并添加默认城市
                    checkAndAddDefaultCity()
                }
            }
        }
    }
    
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                lifecycleScope.launch {
                    // 更新或添加当前位置城市
                    val currentLocationCity = CityInfo(
                        name = "当前位置",
                        location = "${location.longitude},${location.latitude}",
                        isCurrentLocation = true,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    
                    try {
                        repository.updateOrAddCurrentLocationCity(currentLocationCity)
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "当前位置已更新", Toast.LENGTH_SHORT).show()
                            // 更新桌面widget
                            updateWeatherWidgets()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "更新当前位置失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "无法获取当前位置", Toast.LENGTH_SHORT).show()
                checkAndAddDefaultCity()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "位置获取失败: ${it.message}", Toast.LENGTH_SHORT).show()
            checkAndAddDefaultCity()
        }
    }
    
    private fun checkAndAddDefaultCity() {
        lifecycleScope.launch {
            try {
                // 检查是否已有城市
                val hasCities = repository.hasCities()
                if (!hasCities) {
                    addDefaultCity()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "检查城市状态失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun addDefaultCity() {
        lifecycleScope.launch {
            try {
                // 先尝试通过城市搜索API添加北京
                val result = repository.searchAndAddCity("北京")
                
                if (!result.isSuccess) {
                    // 如果搜索失败，使用固定的北京城市ID
                    val defaultCity = CityInfo(
                        name = "北京",
                        location = "101010100", // 北京的和风天气城市ID
                        isCurrentLocation = false,
                        latitude = 39.904200,
                        longitude = 116.407396
                    )
                    
                    repository.addCity(defaultCity)
                }
                
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "已添加默认城市", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "添加默认城市失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun loadCities() {
        lifecycleScope.launch {
            try {
                repository.getAllCities().collect { cities ->
                    pagerAdapter.updateCities(cities)
                    updatePageIndicator(binding.viewPager.currentItem)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "加载城市失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updatePageIndicator(position: Int) {
        try {
            val totalCount = pagerAdapter.itemCount
            if (totalCount > 0 && position >= 0 && position < totalCount) {
                val city = pagerAdapter.getCityAt(position)
                if (city != null) {
                    binding.textPageIndicator.text = "${position + 1} / $totalCount"
                    binding.textCityName.text = city.name
                } else {
                    binding.textPageIndicator.text = "1 / 1"
                    binding.textCityName.text = "天气"
                }
            } else {
                binding.textPageIndicator.text = "1 / 1"
                binding.textCityName.text = "天气"
            }
        } catch (e: Exception) {
            binding.textPageIndicator.text = "1 / 1"
            binding.textCityName.text = "天气"
        }
    }
    
    private fun refreshCurrentPage() {
        lifecycleScope.launch {
            try {
                // 清除缓存以强制刷新
                repository.clearAllCache()
                Toast.makeText(this@MainActivity, "正在刷新天气数据...", Toast.LENGTH_SHORT).show()
                
                // 触发当前Fragment重新加载数据
                val currentPosition = binding.viewPager.currentItem
                val totalCount = pagerAdapter.itemCount
                
                if (totalCount > 0 && currentPosition < totalCount) {
                    // 通知适配器数据发生变化，触发Fragment重新加载
                    pagerAdapter.notifyDataSetChanged()
                    
                    // 更新桌面widget
                    updateWeatherWidgets()
                    
                    Toast.makeText(this@MainActivity, "天气数据已刷新", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "刷新失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showMenuPopup() {
        val popup = PopupMenu(this, binding.buttonMenu)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
        
        // 隐藏已有专门按钮的选项
        popup.menu.findItem(R.id.action_add_city)?.isVisible = false
        popup.menu.findItem(R.id.action_refresh)?.isVisible = false
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_api_test -> {
                    startActivity(Intent(this, ApiTestActivity::class.java))
                    true
                }

                R.id.action_clear_cache -> {
                    clearDatabaseCache()
                    true
                }
                R.id.action_update_location -> {
                    updateCurrentLocation()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
    
    private fun showAddCityDialog() {
        // 创建一个输入框
        val input = android.widget.EditText(this)
        input.hint = "请输入城市名称（如：北京、上海、New York）"
        input.setPadding(50, 30, 50, 30)
        
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("添加城市")
            .setMessage("支持中英文城市名称搜索")
            .setView(input)
            .setPositiveButton("搜索并添加") { _, _ ->
                val cityName = input.text.toString().trim()
                if (cityName.isNotEmpty()) {
                    addCity(cityName)
                } else {
                    Toast.makeText(this, "请输入城市名称", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("常用城市") { _, _ ->
                showCommonCitiesDialog()
            }
            .setNegativeButton("取消", null)
            .create()
        
        dialog.show()
        
        // 自动弹出键盘
        input.requestFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    
    private fun showCommonCitiesDialog() {
        val cityNames = arrayOf(
            "上海", "广州", "深圳", "杭州", "南京", "苏州", 
            "天津", "重庆", "成都", "西安", "武汉", "长沙",
            "郑州", "济南", "青岛", "大连", "沈阳", "哈尔滨",
            "昆明", "贵阳", "兰州", "银川", "乌鲁木齐", "拉萨"
        )
        
        android.app.AlertDialog.Builder(this)
            .setTitle("选择常用城市")
            .setItems(cityNames) { _, which ->
                val selectedCity = cityNames[which]
                addCity(selectedCity)
            }
            .setNegativeButton("返回", null)
            .show()
    }
    
    private fun addCity(cityName: String) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "正在搜索城市: $cityName...", Toast.LENGTH_SHORT).show()
                
                val searchResult = repository.searchCities(cityName)
                
                runOnUiThread {
                    if (searchResult.isSuccess) {
                        val cities = searchResult.getOrNull()
                        if (!cities.isNullOrEmpty()) {
                            if (cities.size == 1) {
                                // 只有一个结果，直接添加
                                addCityToDatabase(cities[0])
                            } else {
                                // 多个结果，让用户选择
                                showCitySelectionDialog(cities)
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "未找到城市: $cityName", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val error = searchResult.exceptionOrNull()
                        Toast.makeText(this@MainActivity, "搜索城市失败: ${error?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "搜索城市失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showCitySelectionDialog(cities: List<CityInfo>) {
        val cityNames = cities.map { it.name }.toTypedArray()
        
        android.app.AlertDialog.Builder(this)
            .setTitle("找到多个城市，请选择")
            .setItems(cityNames) { _, which ->
                addCityToDatabase(cities[which])
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun addCityToDatabase(cityInfo: CityInfo) {
        lifecycleScope.launch {
            try {
                val cityId = repository.addCity(cityInfo)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "已添加 ${cityInfo.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "添加城市失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateCurrentLocation() {
        android.app.AlertDialog.Builder(this)
            .setTitle("更新当前位置")
            .setMessage("这将获取您的最新位置并更新当前位置的天气信息。")
            .setPositiveButton("更新") { _, _ ->
                Toast.makeText(this, "正在获取当前位置...", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun clearDatabaseCache() {
        android.app.AlertDialog.Builder(this)
            .setTitle("清理缓存")
            .setMessage("这将清除所有缓存的天气数据，但保留城市列表。确定继续吗？")
            .setPositiveButton("确定") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.clearAllCache()
                        Toast.makeText(this@MainActivity, "缓存清理成功", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "清理缓存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun updateWeatherWidgets() {
        try {
            WeatherWidgetProvider.updateAllWidgets(this)
        } catch (e: Exception) {
            // 静默处理widget更新失败，不影响主要功能
        }
    }
}