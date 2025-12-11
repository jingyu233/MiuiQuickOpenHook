package com.jingyu233.miuiquickopenhook.hook

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Region
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import com.jingyu233.miuiquickopenhook.data.CustomIconConfig
import com.jingyu233.miuiquickopenhook.data.IntentAction
import com.jingyu233.miuiquickopenhook.data.LaunchType
import com.miui.keyguard.biometrics.fod.item.IQuickOpenItem

/**
 * 自定义快捷图标实现
 * 真实继承 SystemUI 的 IQuickOpenItem（通过 stub + compileOnly）
 * 运行时自然就是正确类型，不会有 ClassCastException
 */
class CustomQuickOpenItem(
    private val config: CustomIconConfig,
    rectF: RectF,
    region: Region,
    context: Context
) : IQuickOpenItem(rectF, region, context) {

    /**
     * 获取 Intent
     * 根据配置动态生成
     */
    override fun getIntent(): Intent {
        // Root 模式：仅使用包名和 Activity
        if (config.useRoot) {
            return Intent().apply {
                component = ComponentName(
                    config.packageName,
                    config.activityName ?: "${config.packageName}.MainActivity"
                )
                if (config.useNewTask) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }

        // 普通模式：完整 Intent 配置
        val actionString = when (config.action) {
            IntentAction.CUSTOM -> config.customAction ?: Intent.ACTION_MAIN
            else -> config.action.value
        }

        return Intent(actionString).apply {
            // 设置包名和组件
            when (config.launchType) {
                LaunchType.ACTIVITY -> {
                    if (!config.activityName.isNullOrEmpty()) {
                        component = ComponentName(config.packageName, config.activityName)
                    } else {
                        setPackage(config.packageName)
                    }
                }
                LaunchType.SERVICE, LaunchType.BROADCAST -> {
                    if (!config.activityName.isNullOrEmpty()) {
                        component = ComponentName(config.packageName, config.activityName)
                    } else {
                        setPackage(config.packageName)
                    }
                }
            }

            // 设置 data URI
            config.data?.takeIf { it.isNotEmpty() }?.let {
                data = Uri.parse(it)
            }

            // 设置 extras (仅支持 String 类型，可扩展)
            config.extras.forEach { (key, value) ->
                putExtra(key, value)
            }

            // 设置 flags
            if (config.useNewTask && config.launchType == LaunchType.ACTIVITY) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * 获取主标题
     */
    override fun getTitle(): String = config.title ?: ""

    /**
     * 获取副标题
     */
    override fun getSubTitle(): String = config.subtitle ?: ""

    /**
     * 获取标签
     */
    override fun getTag(): String = "custom_${config.id}"

    /**
     * 获取 View
     * 创建一个显示应用图标的 View
     */
    override fun getView(): View {
        return CustomIconView(mContext, config)
    }

    /**
     * 自定义图标 View
     * 显示应用图标 + 圆形背景
     */
    private class CustomIconView(
        context: Context,
        private val config: CustomIconConfig
    ) : View(context) {

        private val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        private var appIcon: Drawable? = null

        init {
            // 尝试加载应用图标
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(config.packageName, 0)
                appIcon = pm.getApplicationIcon(appInfo)
            } catch (e: Exception) {
                // 如果无法加载应用图标，使用默认图标
                appIcon = null
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val centerX = width / 2f
            val centerY = height / 2f
            val radius = minOf(width, height) / 2f  // 改为100%，完全填充

            // 绘制圆形背景
            paint.color = Color.argb(180, 255, 255, 255)
            canvas.drawCircle(centerX, centerY, radius, paint)

            // 绘制应用图标 - 完全填充圆形区域
            if (appIcon != null) {
                val iconSize = minOf(width, height).toInt()  // 图标大小填满整个区域
                appIcon?.setBounds(0, 0, iconSize, iconSize)
                appIcon?.draw(canvas)
            } else {
                // 如果没有图标，绘制一个简单的圆形占位符
                paint.color = Color.argb(200, 100, 100, 100)
                canvas.drawCircle(centerX, centerY, radius * 0.6f, paint)
            }
        }
    }
}
