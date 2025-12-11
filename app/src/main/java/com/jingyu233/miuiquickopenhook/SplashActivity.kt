package com.jingyu233.miuiquickopenhook

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

/**
 * 启动页面 - 快速跳转到主页面
 * 无论桌面图标是否隐藏，都从这里启动
 */
private const val TAG = "[GXZW-MiuiQuickOpenHook]"

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "SplashActivity onCreate called")

        try {
            // 直接跳转到MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            Log.d(TAG, "MainActivity started successfully")

            // 关闭SplashActivity
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting MainActivity: ${e.message}")
            // 如果启动失败，设置一个简单的布局作为备用
            setContentView(android.R.layout.simple_list_item_1)
            findViewById<android.widget.TextView>(android.R.id.text1).text = "启动失败: ${e.message}"
        }
    }
}