package com.jingyu233.miuiquickopenhook.config

/**
 * Application Configuration Constants
 *
 * This object centralizes all constants used throughout the application
 * to avoid hardcoding magic numbers and strings.
 */
object AppConfig {

    // ============================================================
    // LOGGING
    // ============================================================

    /** Main logging tag */
    const val LOG_TAG = "QuickOpenHook"

    /** Log tag suffix for configuration-related messages */
    const val LOG_TAG_CONFIG = "QuickOpenHook-Config"

    /** Log tag suffix for custom icon-related messages */
    const val LOG_TAG_CUSTOM = "QuickOpenHook-Custom"

    /** Log tag suffix for error messages */
    const val LOG_TAG_ERROR = "QuickOpenHook-Error"

    /** Log tag suffix for debugging messages */
    const val LOG_TAG_DEBUG = "QuickOpenHook-DEBUG"


    // ============================================================
    // PACKAGE NAMES
    // ============================================================

    /** SystemUI package name */
    const val TARGET_PACKAGE = "com.android.systemui"

    /** This module's package name */
    const val MODULE_PACKAGE = "com.jingyu233.miuiquickopenhook"

    /** SystemUI QuickOpenView class name */
    const val QUICK_OPEN_VIEW_CLASS = "com.miui.keyguard.biometrics.fod.MiuiGxzwQuickOpenView"

    /** SystemUI QuickOpenUtil class name */
    const val QUICK_OPEN_UTIL_CLASS = "com.miui.keyguard.biometrics.fod.MiuiGxzwQuickOpenUtil"

    /** WechatPayItem class name - used as template for custom items */
    val WECHAT_PAY_ITEM_CLASS = "com.miui.keyguard.biometrics.fod.item.WechatPayItem"


    // ============================================================
    // ICON ID RANGES
    // ============================================================

    /** Minimum system icon ID (inclusive) */
    const val SYSTEM_ICON_ID_MIN = 1001

    /** Maximum system icon ID (inclusive) */
    const val SYSTEM_ICON_ID_MAX = 1008

    /** Minimum custom icon ID (inclusive) */
    const val CUSTOM_ICON_ID_MIN = 2001

    /** Maximum custom icon ID (inclusive) */
    const val CUSTOM_ICON_ID_MAX = 9999


    // ============================================================
    // LAYOUT CONSTANTS
    // ============================================================

    /** Default horizontal margin in dp for custom rows */
    const val DEFAULT_HORIZONTAL_MARGIN_DP = 60f

    /** Default vertical spacing in dp from screen bottom */
    const val DEFAULT_VERTICAL_SPACING_DP = 140f

    /** Icon background alpha value (0-255) */
    const val ICON_BACKGROUND_ALPHA = 180

    /** Icon background color (white) */
    const val ICON_BACKGROUND_COLOR_RGB = 255

    /** Fallback icon background alpha value (0-255) */
    const val FALLBACK_ICON_BACKGROUND_ALPHA = 200

    /** Fallback icon background color (gray) */
    const val FALLBACK_ICON_BACKGROUND_COLOR_RGB = 100

    /** Default row 1 icon count */
    const val DEFAULT_ROW_1_ICON_COUNT = 5

    /** Default fan touch region enabled */
    const val DEFAULT_USE_FAN_TOUCH_REGION = true


    // ============================================================
    // SHARED PREFERENCES
    // ============================================================

    /** Module configuration preferences file name */
    const val PREFS_CONFIG_NAME = "miui_config"

    /** Module status preferences file name */
    const val PREFS_MODULE_STATUS_NAME = "module_status"

    /** Key for Xposed activation status */
    const val KEY_XPOSED_ACTIVE = "xposed_active"

    /** Key for row configurations JSON */
    const val KEY_ROW_CONFIGS = "row_configs"

    /** Key for custom icons JSON */
    const val KEY_CUSTOM_ICONS = "custom_icons"


    // ============================================================
    // METHOD NAMES
    // ============================================================

    /** Application onCreate method name */
    const val METHOD_ON_CREATE = "onCreate"

    /** Start show animation method name */
    const val METHOD_START_SHOW_ANIMATION = "startShowQuickOpenItemAnimation"

    /** Generate quick open item method name */
    const val METHOD_GENERATE_QUICK_OPEN_ITEM = "generateQuickOpenItem"

    /** Clean quick open item list method name */
    const val METHOD_CLEAN_QUICK_OPEN_ITEM_LIST = "cleanQuickOpenItemList"

    /** Get intent method name */
    const val METHOD_GET_INTENT = "getIntent"

    /** Get title method name */
    const val METHOD_GET_TITLE = "getTitle"

    /** Get subtitle method name */
    const val METHOD_GET_SUBTITLE = "getSubTitle"

    /** Get tag method name */
    const val METHOD_GET_TAG = "getTag"

    /** Get view method name */
    const val METHOD_GET_VIEW = "getView"


    // ============================================================
    // FIELD NAMES
    // ============================================================

    /** Quick open item list field name */
    const val FIELD_M_QUICK_OPEN_ITEM_LIST = "mQuickOpenItemList"

    /** RectF field name */
    const val FIELD_M_RECT_F = "mRectF"

    /** Region field name */
    const val FIELD_M_REGION = "mRegion"

    /** View field name */
    const val FIELD_M_VIEW = "mView"


    // ============================================================
    // TIMESTAMP FORMAT
    // ============================================================

    /** Date format for timestamps in logs */
    const val TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"

    /** Default locale for date formatting */
    const val DEFAULT_LOCALE = ""


    // ============================================================
    // DEFAULT CONFIGURATIONS
    // ============================================================

    /** Default system icons for row 1 */
    val DEFAULT_ROW_1_ICONS = listOf(
        SYSTEM_ICON_ID_MIN,
        SYSTEM_ICON_ID_MIN + 1,
        SYSTEM_ICON_ID_MIN + 2,
        SYSTEM_ICON_ID_MIN + 3,
        SYSTEM_ICON_ID_MIN + 4
    )

    /** Default system icons for row 2 */
    val DEFAULT_ROW_2_ICONS = listOf(
        SYSTEM_ICON_ID_MAX - 2,
        SYSTEM_ICON_ID_MAX - 1,
        SYSTEM_ICON_ID_MAX,
        SYSTEM_ICON_ID_MIN + 2
    )


    // ============================================================
    // ICON TYPES (for reference)
    // ============================================================

    /** Icon type: Application icon */
    const val ICON_TYPE_APP = "app_icon"

    /** Icon type: Custom image (Base64) */
    const val ICON_TYPE_CUSTOM_IMAGE = "custom_image"

    /** Icon type: System resource */
    const val ICON_TYPE_SYSTEM_RES = "system_res"


    // ============================================================
    // INTENT ACTIONS (for reference)
    // ============================================================

    /** Intent action for main activity */
    const val INTENT_ACTION_MAIN = "android.intent.action.MAIN"

    /** Intent flag for new task */
    const val INTENT_FLAG_NEW_TASK = "android.intent.FLAG_ACTIVITY_NEW_TASK"


    // ============================================================
    // VALIDATION CONSTANTS
    // ============================================================

    /** Maximum JSON config size in characters */
    const val MAX_JSON_SIZE = 100_000

    /** Maximum custom icon size in bytes */
    const val MAX_ICON_SIZE_BYTES = 5_000_000

    /** Base64 decode flags */
    const val BASE64_DECODE_FLAGS = android.util.Base64.DEFAULT
}
