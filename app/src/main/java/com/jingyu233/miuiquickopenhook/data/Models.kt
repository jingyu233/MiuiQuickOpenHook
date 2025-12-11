package com.jingyu233.miuiquickopenhook.data

import android.graphics.RectF
import android.graphics.Region

/**
 * 行配置数据类
 */
data class RowConfig(
    val id: String,
    val enabled: Boolean = true,
    val verticalOffset: Float = 1.4f,
    val itemIds: List<Int> = emptyList(),
    val horizontalMargin: Float = 60f,  // 水平边距（dp）
    val verticalSpacing: Float = 140f,  // 距离屏幕底部的距离（dp）
    val useFanTouchRegion: Boolean = true  // 是否使用扇形触摸区域
)

/**
 * 位置信息数据类 - 用于保存item的RectF和Region
 */
data class PositionInfo(
    val rect: RectF,
    val region: Region
)

/**
 * 布局参考值数据类 - 持久化保存
 */
data class LayoutReference(
    val xMin: Float,           // 左边界
    val xMax: Float,           // 右边界
    val baseY: Float,          // 基准Y坐标
    val itemWidth: Float,      // 图标宽度
    val itemHeight: Float,     // 图标高度
    val screenWidth: Int,      // 屏幕宽度
    val timestamp: Long        // 保存时间戳
) {
    fun toJson(): String {
        return """
        {
            "xMin": $xMin,
            "xMax": $xMax,
            "baseY": $baseY,
            "itemWidth": $itemWidth,
            "itemHeight": $itemHeight,
            "screenWidth": $screenWidth,
            "timestamp": $timestamp
        }
        """.trimIndent()
    }

    companion object {
        fun fromJson(json: String): LayoutReference? {
            return try {
                val obj = org.json.JSONObject(json)
                LayoutReference(
                    xMin = obj.getDouble("xMin").toFloat(),
                    xMax = obj.getDouble("xMax").toFloat(),
                    baseY = obj.getDouble("baseY").toFloat(),
                    itemWidth = obj.getDouble("itemWidth").toFloat(),
                    itemHeight = obj.getDouble("itemHeight").toFloat(),
                    screenWidth = obj.getInt("screenWidth"),
                    timestamp = obj.getLong("timestamp")
                )
            } catch (e: Exception) {
                null
            }
        }

        // 硬编码的默认值 (基于1080p屏幕)
        fun getDefault(screenWidth: Int): LayoutReference {
            val scale = screenWidth / 1080f
            return LayoutReference(
                xMin = 200f * scale,
                xMax = 880f * scale,
                baseY = 1800f * scale,
                itemWidth = 120f * scale,
                itemHeight = 120f * scale,
                screenWidth = screenWidth,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
