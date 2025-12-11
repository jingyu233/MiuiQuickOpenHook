package com.jingyu233.miuiquickopenhook.data

import org.json.JSONObject

/**
 * 启动方式枚举
 */
enum class LaunchType(val value: String) {
    ACTIVITY("activity"),       // 启动Activity
    SERVICE("service"),          // 启动Service (Foreground)
    BROADCAST("broadcast");      // 发送广播

    companion object {
        fun fromValue(value: String): LaunchType {
            return values().find { it.value == value } ?: ACTIVITY
        }
    }
}

/**
 * Intent Action 类型
 */
enum class IntentAction(val value: String, val displayName: String) {
    MAIN("android.intent.action.MAIN", "MAIN (默认)"),
    VIEW("android.intent.action.VIEW", "VIEW (查看)"),
    SEND("android.intent.action.SEND", "SEND (发送)"),
    CUSTOM("", "自定义");

    companion object {
        fun fromValue(value: String): IntentAction {
            return values().find { it.value == value } ?: CUSTOM
        }
    }
}

/**
 * 图标类型
 */
enum class IconType(val value: String) {
    SYSTEM_RES("system_res"),      // 系统drawable资源（仅1001-1008）
    APP_ICON("app_icon"),          // 应用图标
    CUSTOM_IMAGE("custom_image");  // 自定义图片

    companion object {
        fun fromValue(value: String): IconType {
            return values().find { it.value == value } ?: APP_ICON
        }
    }
}

/**
 * 自定义图标配置
 * 用于 1001-1008 和 2001-9999 范围的图标
 */
data class CustomIconConfig(
    val id: Int,                                    // 1001-1008 或 2001-9999
    val enabled: Boolean = true,                    // 是否启用
    val title: String,                              // 主标题
    val subtitle: String,                           // 副标题

    // 图标配置
    val iconType: IconType = IconType.APP_ICON,     // 图标类型
    val iconData: String? = null,                   // 图标数据（根据iconType不同含义不同）
                                                    // SYSTEM_RES: drawable资源名（如"gxzw_quick_open_wechat_pay"）
                                                    // APP_ICON: 应用包名（如"com.tencent.mm"）
                                                    // CUSTOM_IMAGE: Base64编码的图片数据

    // Intent 基础配置
    val launchType: LaunchType = LaunchType.ACTIVITY,
    val packageName: String,                        // 包名 (必填)
    val activityName: String? = null,               // Activity/Service/Receiver 完整类名 (可选)

    // Intent 高级配置
    val data: String? = null,                       // Intent data URI
    val extras: Map<String, String> = emptyMap(),   // Intent extras (String类型)
    val action: IntentAction = IntentAction.MAIN,   // Intent action
    val customAction: String? = null,               // 自定义 action (当 action=CUSTOM 时使用)

    // 标志位
    val useNewTask: Boolean = true,                 // FLAG_ACTIVITY_NEW_TASK
    val useRoot: Boolean = false                    // 使用 Root 启动 (仅识别包名和Activity，忽略其他参数)
) {
    /**
     * 转换为 JSON
     */
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("enabled", enabled)
        json.put("title", title)
        json.put("subtitle", subtitle)

        // 图标配置
        json.put("iconType", iconType.value)
        if (!iconData.isNullOrEmpty()) {
            json.put("iconData", iconData)
        }

        json.put("launchType", launchType.value)
        json.put("packageName", packageName)
        if (!activityName.isNullOrEmpty()) {
            json.put("activityName", activityName)
        }
        if (!data.isNullOrEmpty()) {
            json.put("data", data)
        }
        val extrasJson = JSONObject()
        extras.forEach { (k, v) -> extrasJson.put(k, v) }
        json.put("extras", extrasJson)
        json.put("action", action.value)
        if (!customAction.isNullOrEmpty()) {
            json.put("customAction", customAction)
        }
        json.put("useNewTask", useNewTask)
        json.put("useRoot", useRoot)
        return json  // 返回JSONObject，不是String
    }

    companion object {
        /**
         * 从 JSON 解析
         */
        fun fromJson(jsonObj: JSONObject): CustomIconConfig {
            val extrasObj = jsonObj.optJSONObject("extras")
            val extras = mutableMapOf<String, String>()
            if (extrasObj != null) {
                extrasObj.keys().forEach { key ->
                    extras[key] = extrasObj.getString(key)
                }
            }

            val actionValue = jsonObj.optString("action", IntentAction.MAIN.value)
            val intentAction = IntentAction.fromValue(actionValue)

            val iconTypeValue = jsonObj.optString("iconType", IconType.APP_ICON.value)
            val iconType = IconType.fromValue(iconTypeValue)

            return CustomIconConfig(
                id = jsonObj.getInt("id"),
                enabled = jsonObj.optBoolean("enabled", true),
                title = jsonObj.getString("title"),
                subtitle = jsonObj.optString("subtitle", ""),
                iconType = iconType,
                iconData = jsonObj.optString("iconData", "").takeIf { it.isNotEmpty() },
                launchType = LaunchType.fromValue(jsonObj.optString("launchType", "activity")),
                packageName = jsonObj.getString("packageName"),
                activityName = jsonObj.optString("activityName", "").takeIf { it.isNotEmpty() },
                data = jsonObj.optString("data", "").takeIf { it.isNotEmpty() },
                extras = extras,
                action = intentAction,
                customAction = jsonObj.optString("customAction", "").takeIf { it.isNotEmpty() },
                useNewTask = jsonObj.optBoolean("useNewTask", true),
                useRoot = jsonObj.optBoolean("useRoot", false)
            )
        }
    }
}
