package com.example.test4.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {
    
    private val preferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREF_NAME = "note_app_preferences"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USERNAME = "username"
        private const val KEY_SORT_ORDER = "sort_order"
        private const val KEY_SELECTED_CATEGORY = "selected_category"
    }
    
    // 登录状态管理
    fun setLoggedIn(isLoggedIn: Boolean) {
        preferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }
    
    fun isLoggedIn(): Boolean {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    // 用户名管理
    fun setUsername(username: String) {
        preferences.edit().putString(KEY_USERNAME, username).apply()
    }
    
    fun getUsername(): String {
        return preferences.getString(KEY_USERNAME, "") ?: ""
    }
    
    // 排序设置
    fun setSortOrder(sortOrder: String) {
        preferences.edit().putString(KEY_SORT_ORDER, sortOrder).apply()
    }
    
    fun getSortOrder(): String {
        return preferences.getString(KEY_SORT_ORDER, "updated_desc") ?: "updated_desc"
    }
    
    // 类别筛选
    fun setSelectedCategory(category: String) {
        preferences.edit().putString(KEY_SELECTED_CATEGORY, category).apply()
    }
    
    fun getSelectedCategory(): String {
        return preferences.getString(KEY_SELECTED_CATEGORY, "全部") ?: "全部"
    }
    
    // 清除所有数据
    fun clearAll() {
        preferences.edit().clear().apply()
    }
} 