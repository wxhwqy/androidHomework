package com.example.test4.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test4.MainActivity
import com.example.test4.databinding.ActivityLoginBinding
import com.example.test4.utils.PreferencesHelper

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var preferencesHelper: PreferencesHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesHelper = PreferencesHelper(this)
        
        if (preferencesHelper.isLoggedIn()) {
            navigateToMain()
            return
        }
        
        setupViews()
    }
    
    private fun setupViews() {
        binding.btnLogin.setOnClickListener {
            handleLogin()
        }
        
        binding.tvRegister.setOnClickListener {
            Toast.makeText(this, "注册功能未实现，请使用 admin（用户名）/123456（密码） 登录", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun handleLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        if (username.isEmpty()) {
            binding.tilUsername.error = "请输入用户名"
            return
        }
        
        if (password.isEmpty()) {
            binding.tilPassword.error = "请输入密码"
            return
        }
        
        binding.tilUsername.error = null
        binding.tilPassword.error = null
        
        if (username == "admin" && password == "123456") {
            if (binding.cbRememberMe.isChecked) {
                preferencesHelper.setLoggedIn(true)
                preferencesHelper.setUsername(username)
                Toast.makeText(this, "登录成功，已记住登录状态", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
            }
            
            navigateToMain()
        } else {
            Toast.makeText(this, "用户名或密码错误", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("from_login", true)
        startActivity(intent)
        finish()
    }
} 