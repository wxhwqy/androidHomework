package com.example.homework3.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.homework3.data.CityInfo

class WeatherPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    
    private var cities: List<CityInfo> = emptyList()
    
    fun updateCities(newCities: List<CityInfo>) {
        cities = newCities
        notifyDataSetChanged()
    }
    
    override fun getItemCount(): Int = cities.size
    
    override fun createFragment(position: Int): Fragment {
        return if (position in cities.indices) {
            val city = cities[position]
            WeatherFragment.newInstance(
                location = city.location,
                cityName = city.name,
                isCurrentLocation = city.isCurrentLocation
            )
        } else {
            // 如果索引无效，返回一个默认的Fragment
            WeatherFragment.newInstance(
                location = "beijing",
                cityName = "北京",
                isCurrentLocation = false
            )
        }
    }
    
    fun getCityAt(position: Int): CityInfo? {
        return if (position in cities.indices) cities[position] else null
    }
} 