package com.jingyu233.miuiquickopenhook.data

import android.graphics.drawable.Drawable

/**
 * 应用信息数据类
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val launchActivity: String?  // 主Activity类名
)
