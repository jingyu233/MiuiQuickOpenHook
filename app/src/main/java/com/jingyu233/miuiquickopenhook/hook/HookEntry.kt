package com.jingyu233.miuiquickopenhook.hook

import android.content.Context
import android.content.Intent
import android.graphics.RectF
import android.graphics.Region
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.jingyu233.miuiquickopenhook.config.AppConfig
import com.jingyu233.miuiquickopenhook.data.LayoutReference
import com.jingyu233.miuiquickopenhook.data.PositionInfo
import com.jingyu233.miuiquickopenhook.data.RowConfig
import com.jingyu233.miuiquickopenhook.data.CustomIconConfig
import com.jingyu233.miuiquickopenhook.data.IntentAction
import com.jingyu233.miuiquickopenhook.data.LaunchType
import com.jingyu233.miuiquickopenhook.manager.PreferencesManager
import com.jingyu233.miuiquickopenhook.util.XposedLogger
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Field

/**
 * Main Xposed Hook entry point for MIUI Quick Open module
 *
 * This class handles the initialization and hooking of SystemUI's
 * MiuiGxzwQuickOpenView to customize the quick open items layout.
 */
class HookEntry : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Mark module as active in the app's own process
        if (lpparam.packageName == AppConfig.MODULE_PACKAGE) {
            hookModuleApplication(lpparam)
            return
        }

        // Only hook SystemUI
        if (lpparam.packageName != AppConfig.TARGET_PACKAGE) {
            return
        }

        XposedLogger.info("Module loaded in process: ${lpparam.processName}")
        XposedLogger.info("Package: ${lpparam.packageName}")

        try {
            hookQuickOpenView(lpparam)
            XposedLogger.hookCompleted()
        } catch (e: Throwable) {
            XposedLogger.hookFailed(e.message ?: "Unknown error", e)
        }
    }

    /**
     * Hook Application.onCreate to mark module as active
     */
    private fun hookModuleApplication(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedLogger.executeWithLogging("Module application hook") {
            XposedHelpers.findAndHookMethod(
                "android.app.Application",
                lpparam.classLoader,
                AppConfig.METHOD_ON_CREATE,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        XposedLogger.executeWithLogging("Mark module active") {
                            val context = param.thisObject as Context
                            val prefs = context.getSharedPreferences(
                                AppConfig.PREFS_MODULE_STATUS_NAME,
                                Context.MODE_PRIVATE
                            )
                            prefs.edit().putBoolean(AppConfig.KEY_XPOSED_ACTIVE, true).apply()
                            XposedLogger.info("Module status marked as active")
                        }
                    }
                }
            )
        }
    }

    /**
     * Hook MiuiGxzwQuickOpenView class methods
     */
    private fun hookQuickOpenView(lpparam: XC_LoadPackage.LoadPackageParam) {
        val quickOpenViewClass = XposedHelpers.findClassIfExists(
            AppConfig.QUICK_OPEN_VIEW_CLASS,
            lpparam.classLoader
        ) ?: run {
            XposedLogger.warning("MiuiGxzwQuickOpenView class not found")
            return
        }

        XposedLogger.info("Found MiuiGxzwQuickOpenView class")

        // Debug: list all method signatures (optional)
        XposedLogger.executeWithLogging("Dump methods") {
            quickOpenViewClass.declaredMethods.forEach { method ->
                XposedLogger.debug(
                    "Method: ${method.name}(${method.parameterTypes.joinToString { it.simpleName }})"
                )
            }
        }

        // Hook startShowQuickOpenItemAnimation method
        hookStartShowAnimation(lpparam, quickOpenViewClass)
    }

    /**
     * Hook startShowQuickOpenItemAnimation method
     */
    private fun hookStartShowAnimation(
        lpparam: XC_LoadPackage.LoadPackageParam,
        quickOpenViewClass: Class<*>
    ) {
        XposedLogger.executeWithLogging("Hook startShowAnimation") {
            XposedHelpers.findAndHookMethod(
                quickOpenViewClass,
                AppConfig.METHOD_START_SHOW_ANIMATION,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        // Let system execute normally:
                        // 1. cleanQuickOpenItemList() - clean old Views
                        // 2. generate items (according to DEFAULT_ITEM_ID_LIST)
                        // 3. addView() - add View to ViewGroup
                        XposedLogger.debug("Before: Let system execute normally")
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        XposedLogger.executeWithLogging("Handle animation logic") {
                            handleStartShowAnimationLogic(param, lpparam.classLoader)
                        }
                    }
                }
            )
            XposedLogger.hookSuccess(AppConfig.METHOD_START_SHOW_ANIMATION)
        }
    }

    /**
     * Unified creation of QuickOpenItem
     * Prioritizes custom configuration, otherwise calls system method
     *
     * @param itemId Icon ID (1001-1008 or 2001-9999)
     * @param rect Icon position and size
     * @param region Touch region
     * @param context SystemUI Context
     * @param classLoader SystemUI ClassLoader
     * @return IQuickOpenItem instance (system or custom), null on failure
     */
    private fun createQuickOpenItem(
        itemId: Int,
        rect: RectF,
        region: Region,
        context: Context,
        classLoader: ClassLoader
    ): Any? {
        // 1. Try to load custom configuration
        val customConfig = loadCustomIconConfig(itemId)

        if (customConfig != null && customConfig.enabled) {
            XposedLogger.custom(
                "Using custom config for itemId=%d (%s)",
                itemId,
                customConfig.title
            )

            // 2. Use SystemUI ClassLoader to create CustomQuickOpenItem
            return XposedLogger.executeWithLogging(
                "Create custom item",
                null
            ) {
                createCustomQuickOpenItemWithSystemClassLoader(
                    customConfig, rect, region, context, classLoader
                )
            }
        }

        // 3. No configuration → call system generateQuickOpenItem
        return XposedLogger.executeWithLogging(
            "Generate system item for id=$itemId",
            null
        ) {
            val utilClass = classLoader.loadClass(AppConfig.QUICK_OPEN_UTIL_CLASS)
            val genMethod = utilClass.getDeclaredMethod(
                AppConfig.METHOD_GENERATE_QUICK_OPEN_ITEM,
                RectF::class.java,
                Region::class.java,
                Context::class.java,
                Int::class.javaPrimitiveType
            )
            genMethod.isAccessible = true

            genMethod.invoke(null, rect, region, context, itemId)
        } ?: run {
            // IDs in range 2001-9999 without config will reach here
            if (itemId in AppConfig.CUSTOM_ICON_ID_MIN..AppConfig.CUSTOM_ICON_ID_MAX) {
                XposedLogger.info(
                    "Custom itemId=%d has no config and no system fallback (expected)",
                    itemId
                )
            } else {
                XposedLogger.error("Failed to generate system item for id=%d", itemId)
            }
            null
        }
    }

    /**
     * Create IQuickOpenItem instance using dynamic proxy
     * Creates an instance by copying an existing Item and modifying its fields and methods
     * to avoid ClassLoader issues
     */
    private fun createCustomQuickOpenItemWithSystemClassLoader(
        config: CustomIconConfig,
        rect: RectF,
        region: Region,
        context: Context,
        systemClassLoader: ClassLoader
    ): Any {
        // Use WechatPayItem as template (or any other concrete implementation)
        // Create an instance via reflection, then modify its behavior
        val wechatPayItemClass = systemClassLoader.loadClass(AppConfig.WECHAT_PAY_ITEM_CLASS)

        // Create WechatPayItem instance
        val itemInstance = XposedHelpers.newInstance(
            wechatPayItemClass,
            rect, region, context
        )

        // Create custom View
        val customView = createCustomIconView(config, context, rect)

        // Use XposedHelpers to set mView field
        XposedLogger.executeWithLogging("Set mView field") {
            XposedHelpers.setObjectField(itemInstance, AppConfig.FIELD_M_VIEW, customView)
        }

        // Hook getIntent method
        hookItemMethod(
            wechatPayItemClass,
            AppConfig.METHOD_GET_INTENT,
            itemInstance
        ) {
            createIntentFromConfig(config, context)
        }

        // Hook getTitle method
        hookItemMethod(
            wechatPayItemClass,
            AppConfig.METHOD_GET_TITLE,
            itemInstance
        ) {
            config.title
        }

        // Hook getSubTitle method
        hookItemMethod(
            wechatPayItemClass,
            AppConfig.METHOD_GET_SUBTITLE,
            itemInstance
        ) {
            config.subtitle
        }

        // Hook getTag method
        hookItemMethod(
            wechatPayItemClass,
            AppConfig.METHOD_GET_TAG,
            itemInstance
        ) {
            "custom_${config.id}"
        }

        // Hook getView method
        hookItemMethod(
            wechatPayItemClass,
            AppConfig.METHOD_GET_VIEW,
            itemInstance
        ) {
            customView
        }

        XposedLogger.custom("Successfully created custom item proxy for %s", config.title)

        return itemInstance
    }

    /**
     * Helper method to hook a specific method on an item instance
     */
    private fun hookItemMethod(
        itemClass: Class<*>,
        methodName: String,
        itemInstance: Any,
        resultProvider: () -> Any?
    ) {
        val method = itemClass.getDeclaredMethod(methodName)
        XposedBridge.hookMethod(method, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (param.thisObject == itemInstance) {
                    param.result = resultProvider()
                }
            }
        })
    }

    /**
     * 根据配置创建 Intent
     */
    private fun createIntentFromConfig(config: CustomIconConfig, context: Context): Intent {
        // Root 模式：仅使用包名和 Activity（最简化）
        if (config.useRoot) {
            return Intent().apply {
                component = android.content.ComponentName(
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
                        component = android.content.ComponentName(config.packageName, config.activityName)
                    } else {
                        setPackage(config.packageName)
                    }
                }
                LaunchType.SERVICE, LaunchType.BROADCAST -> {
                    if (!config.activityName.isNullOrEmpty()) {
                        component = android.content.ComponentName(config.packageName, config.activityName)
                    } else {
                        setPackage(config.packageName)
                    }
                }
            }

            // 设置 data URI
            config.data?.takeIf { it.isNotEmpty() }?.let {
                data = android.net.Uri.parse(it)
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
     * Create custom icon View
     */
    private fun createCustomIconView(config: CustomIconConfig, context: Context, rect: RectF): View {
        return object : View(context) {
            private val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
            }

            private var icon: android.graphics.drawable.Drawable? = null

            init {
                // Load different types of icons based on iconType
                loadIcon(config, context)
            }

            /**
             * Load icon based on configuration
             */
            private fun loadIcon(config: CustomIconConfig, context: Context) {
                XposedLogger.executeWithLogging("Load icon") {
                    when (config.iconType) {
                        com.jingyu233.miuiquickopenhook.data.IconType.APP_ICON -> {
                            loadAppIcon(config, context)
                        }
                        com.jingyu233.miuiquickopenhook.data.IconType.CUSTOM_IMAGE -> {
                            loadCustomImage(config, context)
                        }
                        com.jingyu233.miuiquickopenhook.data.IconType.SYSTEM_RES -> {
                            XposedLogger.custom("SYSTEM_RES not yet supported")
                        }
                    }
                }
            }

            /**
             * Load application icon
             */
            private fun loadAppIcon(config: CustomIconConfig, context: Context) {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(config.packageName, 0)
                icon = pm.getApplicationIcon(appInfo)
                XposedLogger.custom("Loaded app icon for %s", config.packageName)
            }

            /**
             * Load custom image (Base64)
             */
            private fun loadCustomImage(config: CustomIconConfig, context: Context) {
                config.iconData.takeIf { !it.isNullOrEmpty() }?.let { iconData ->
                    val imageBytes = android.util.Base64.decode(
                        iconData,
                        AppConfig.BASE64_DECODE_FLAGS
                    )
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(
                        imageBytes,
                        0,
                        imageBytes.size
                    )

                    if (bitmap != null) {
                        icon = android.graphics.drawable.BitmapDrawable(
                            context.resources,
                            bitmap
                        )
                        XposedLogger.custom(
                            "Loaded custom image for %s, size: %d bytes",
                            config.title,
                            imageBytes.size
                        )
                    } else {
                        XposedLogger.custom("Failed to decode bitmap for %s", config.title)
                    }
                }
            }

            override fun onDraw(canvas: android.graphics.Canvas) {
                super.onDraw(canvas)

                val centerX = width / 2f
                val centerY = height / 2f
                val radius = minOf(width, height) / 2f

                // Draw circular background (semi-transparent white)
                paint.color = android.graphics.Color.argb(
                    AppConfig.ICON_BACKGROUND_ALPHA,
                    AppConfig.ICON_BACKGROUND_COLOR_RGB,
                    AppConfig.ICON_BACKGROUND_COLOR_RGB,
                    AppConfig.ICON_BACKGROUND_COLOR_RGB
                )
                canvas.drawCircle(centerX, centerY, radius, paint)

                // Draw icon
                if (icon != null) {
                    val iconSize = minOf(width, height).toInt()
                    icon?.setBounds(0, 0, iconSize, iconSize)
                    icon?.draw(canvas)
                } else {
                    // Draw fallback gray circle
                    paint.color = android.graphics.Color.argb(
                        AppConfig.FALLBACK_ICON_BACKGROUND_ALPHA,
                        AppConfig.FALLBACK_ICON_BACKGROUND_COLOR_RGB,
                        AppConfig.FALLBACK_ICON_BACKGROUND_COLOR_RGB,
                        AppConfig.FALLBACK_ICON_BACKGROUND_COLOR_RGB
                    )
                    canvas.drawCircle(centerX, centerY, radius * 0.6f, paint)
                }
            }
        }
    }

    /**
     * Handle startShowQuickOpenItemAnimation core logic
     */
    private fun handleStartShowAnimationLogic(
        param: XC_MethodHook.MethodHookParam,
        classLoader: ClassLoader
    ) {
        val thisObj = param.thisObject

        // Get item list
        val itemList = getItemList(thisObj)
        if (itemList.isEmpty()) {
            XposedLogger.info("itemList is empty, skip")
            return
        }

        // Get field references for first item
        val (rectFieldForRow1, regionFieldForRow1) = getItemFields(itemList)
        if (rectFieldForRow1 == null || regionFieldForRow1 == null) {
            XposedLogger.warning("Cannot find mRectF/mRegion fields for row1")
            return
        }

        // Load configurations
        val allRows = loadConfigsFromBridge()
        val row1Config = allRows.firstOrNull { it.id == "row_1" }
        val row1ItemCount = row1Config?.itemIds?.size ?: AppConfig.DEFAULT_ROW_1_ICON_COUNT

        // Save first row positions
        val firstRowPositions = saveFirstRowPositions(
            itemList,
            rectFieldForRow1,
            regionFieldForRow1,
            row1ItemCount
        )

        // Clean existing views
        cleanQuickOpenItems(thisObj)

        // Get screen metrics
        val context = (thisObj as FrameLayout).context
        val screenMetrics = getScreenMetrics(context)

        XposedLogger.info(
            "Screen info: width=%d, height=%d, density=%.2f",
            screenMetrics.width,
            screenMetrics.height,
            screenMetrics.density
        )

        // Process each row
        val totalItems = processRows(
            allRows,
            itemList,
            firstRowPositions,
            context,
            screenMetrics,
            classLoader
        )

        // Add views to ViewGroup
        addViewsToViewGroup(thisObj, itemList)

        XposedLogger.info("Added %d items to ViewGroup successfully", totalItems)
        XposedLogger.info("Row 1: arc animation, other rows: custom layout")
    }

    /**
     * Get item list from QuickOpenView
     */
    private fun getItemList(thisObj: Any): MutableList<*> {
        val itemListField = thisObj.javaClass.getDeclaredField(AppConfig.FIELD_M_QUICK_OPEN_ITEM_LIST)
        itemListField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return itemListField.get(thisObj) as MutableList<*>
    }

    /**
     * Get rect and region fields from first item
     */
    private fun getItemFields(itemList: MutableList<*>): Pair<Field?, Field?> {
        val firstItemClass = itemList.first()!!.javaClass
        val rectFieldForRow1 = findFieldInHierarchy(firstItemClass, AppConfig.FIELD_M_RECT_F)
        val regionFieldForRow1 = findFieldInHierarchy(firstItemClass, AppConfig.FIELD_M_REGION)
        return Pair(rectFieldForRow1, regionFieldForRow1)
    }

    /**
     * Save first row positions from system
     */
    private fun saveFirstRowPositions(
        itemList: MutableList<*>,
        rectField: Field,
        regionField: Field,
        row1ItemCount: Int
    ): List<PositionInfo> {
        val firstRowPositions = mutableListOf<PositionInfo>()
        for (i in 0 until kotlin.math.min(row1ItemCount, itemList.size)) {
            val item = itemList[i]!!
            val rect = RectF(rectField.get(item) as RectF)
            val region = Region(regionField.get(item) as Region)
            firstRowPositions.add(PositionInfo(rect, region))
        }

        XposedLogger.info("Saved %d positions from system", firstRowPositions.size)
        return firstRowPositions
    }

    /**
     * Clean existing quick open items
     */
    private fun cleanQuickOpenItems(thisObj: Any) {
        val cleanMethod = thisObj.javaClass.getDeclaredMethod(AppConfig.METHOD_CLEAN_QUICK_OPEN_ITEM_LIST)
        cleanMethod.isAccessible = true
        cleanMethod.invoke(thisObj)
        XposedLogger.info("Cleaned all views via cleanQuickOpenItemList()")
    }

    /**
     * Get screen metrics
     */
    private data class ScreenMetrics(
        val width: Int,
        val height: Int,
        val density: Float
    )

    private fun getScreenMetrics(context: Context): ScreenMetrics {
        val metrics = context.resources.displayMetrics
        return ScreenMetrics(
            width = metrics.widthPixels,
            height = metrics.heightPixels,
            density = metrics.density
        )
    }

    /**
     * Process each row configuration
     */
    private fun processRows(
        allRows: List<RowConfig>,
        itemList: MutableList<*>,
        firstRowPositions: List<PositionInfo>,
        context: Context,
        screenMetrics: ScreenMetrics,
        classLoader: ClassLoader
    ): Int {
        var totalItems = 0

        allRows.forEachIndexed { rowIndex, rowConfig ->
            if (!rowConfig.enabled || rowConfig.itemIds.isEmpty()) {
                XposedLogger.info("Row %d disabled or empty, skip", rowIndex + 1)
                return@forEachIndexed
            }

            if (rowIndex != 0) {
                totalItems += processCustomRow(
                    rowIndex,
                    rowConfig,
                    itemList,
                    firstRowPositions,
                    context,
                    screenMetrics,
                    classLoader
                )
            } else {
                totalItems += processRow1(
                    rowConfig,
                    itemList,
                    firstRowPositions,
                    context,
                    classLoader
                )
            }
        }

        return totalItems
    }

    /**
     * Process custom row (not row 1)
     */
    private fun processCustomRow(
        rowIndex: Int,
        rowConfig: RowConfig,
        itemList: MutableList<*>,
        firstRowPositions: List<PositionInfo>,
        context: Context,
        screenMetrics: ScreenMetrics,
        classLoader: ClassLoader
    ): Int {
        val layoutRef = getOrCreateLayoutReference(context, firstRowPositions, screenMetrics.width)

        // Calculate horizontal bounds (full screen width - margin)
        val marginPx = rowConfig.horizontalMargin * screenMetrics.density
        val xMin = marginPx.coerceAtLeast(0f)
        val xMax = (screenMetrics.width - marginPx).coerceAtMost(screenMetrics.width.toFloat())

        // Calculate vertical position (from bottom of screen)
        val distanceFromBottomPx = rowConfig.verticalSpacing * screenMetrics.density
        val rowCenterY = screenMetrics.height - distanceFromBottomPx

        XposedLogger.info("Row %d layout calculation:", rowIndex + 1)
        XposedLogger.info(
            "Config: margin=%.1fdp, distanceFromBottom=%.1fdp",
            rowConfig.horizontalMargin,
            rowConfig.verticalSpacing
        )
        XposedLogger.info("Density: %.2f", screenMetrics.density)
        XposedLogger.info(
            "Horizontal bounds: xMin=%.1f, xMax=%.1f (range=%.1f)",
            xMin, xMax, xMax - xMin
        )
        XposedLogger.info(
            "Vertical position: distanceFromBottom=%.1fpx, rowCenterY=%.1f",
            distanceFromBottomPx, rowCenterY
        )

        var rowItemCount = 0

        rowConfig.itemIds.forEachIndexed { index, itemId ->
            // Distribute evenly within full screen width
            val t = (index + 1).toFloat() / (rowConfig.itemIds.size + 1)
            val cx = xMin + (xMax - xMin) * t
            val cy = rowCenterY

            XposedLogger.info(
                "Icon #%d (id=%d): t=%.2f, cx=%.1f, cy=%.1f",
                index + 1, itemId, t, cx, cy
            )

            val rect = RectF(
                cx - layoutRef.itemWidth / 2,
                cy - layoutRef.itemHeight / 2,
                cx + layoutRef.itemWidth / 2,
                cy + layoutRef.itemHeight / 2
            )

            val region = Region(
                rect.left.toInt(),
                rect.top.toInt(),
                rect.right.toInt(),
                rect.bottom.toInt()
            )

            val item = createQuickOpenItem(itemId, rect, region, context, classLoader)
            if (item != null) {
                @Suppress("UNCHECKED_CAST")
                (itemList as MutableList<Any>).add(0, item)
                rowItemCount++
                XposedLogger.info(
                    "Row %d item #%d inserted at front, total: %d",
                    rowIndex + 1, index, rowItemCount
                )
            }
        }

        XposedLogger.info(
            "Row %d: %d icons (full screen, margin=%.1fpx, distanceFromBottom=%.1fpx, inserted at front)",
            rowIndex + 1,
            rowConfig.itemIds.size,
            marginPx,
            distanceFromBottomPx
        )

        return rowItemCount
    }

    /**
     * Process row 1 (arc layout)
     */
    private fun processRow1(
        rowConfig: RowConfig,
        itemList: MutableList<*>,
        firstRowPositions: List<PositionInfo>,
        context: Context,
        classLoader: ClassLoader
    ): Int {
        var rowItemCount = 0

        rowConfig.itemIds.forEachIndexed { index, itemId ->
            if (index < firstRowPositions.size) {
                val pos = firstRowPositions[index]

                val region = if (rowConfig.useFanTouchRegion) {
                    // Use original fan-shaped Region
                    pos.region
                } else {
                    // Use rectangular Region (only the icon itself)
                    Region(
                        pos.rect.left.toInt(),
                        pos.rect.top.toInt(),
                        pos.rect.right.toInt(),
                        pos.rect.bottom.toInt()
                    )
                }

                val item = createQuickOpenItem(itemId, pos.rect, region, context, classLoader)
                if (item != null) {
                    @Suppress("UNCHECKED_CAST")
                    (itemList as MutableList<Any>).add(item)
                    rowItemCount++
                }
            }
        }

        XposedLogger.info(
            "Row 1: %d items (arc layout, useFanTouchRegion=%s, appended at end)",
            rowConfig.itemIds.size,
            rowConfig.useFanTouchRegion
        )

        return rowItemCount
    }

    /**
     * Add views to ViewGroup
     */
    private fun addViewsToViewGroup(thisObj: Any, itemList: MutableList<*>) {
        val viewGroup = thisObj as ViewGroup
        val addViewMethod = ViewGroup::class.java.getMethod(
            "addView",
            View::class.java,
            ViewGroup.LayoutParams::class.java
        )

        itemList.forEach { item ->
            if (item == null) return@forEach

            val itemClass = item.javaClass

            val itemRectField = findFieldInHierarchy(itemClass, AppConfig.FIELD_M_RECT_F)
            if (itemRectField == null) {
                XposedLogger.warning(
                    "mRectF not found for item class %s, skip addView",
                    itemClass.name
                )
                return@forEach
            }

            val rect = itemRectField.get(item) as? RectF
            if (rect == null) {
                XposedLogger.warning(
                    "rect is null for item class %s, skip addView",
                    itemClass.name
                )
                return@forEach
            }

            val getViewMethod = itemClass.getMethod(AppConfig.METHOD_GET_VIEW)
            val view = getViewMethod.invoke(item) as View

            val layoutParams = FrameLayout.LayoutParams(
                rect.width().toInt(),
                rect.height().toInt()
            )
            layoutParams.gravity = android.view.Gravity.TOP or android.view.Gravity.LEFT
            layoutParams.leftMargin = rect.left.toInt()
            layoutParams.topMargin = rect.top.toInt()

            view.visibility = View.VISIBLE

            addViewMethod.invoke(viewGroup, view, layoutParams)
        }
    }

    /**
     * Load configuration from LSPosed XSharedPreferences
     * Uses New XSharedPreferences mechanism (LSPosed API 93+)
     */
    private fun loadConfigsFromBridge(): List<RowConfig> {
        return XposedLogger.executeWithLogging(
            "Load configs from bridge",
            getDefaultRowConfigs()
        ) {
            val prefs = PreferencesManager.getXSharedPreferences(
                AppConfig.MODULE_PACKAGE,
                AppConfig.PREFS_CONFIG_NAME
            )

            // Check if file is readable
            if (!PreferencesManager.isXSharedPreferencesReadable(prefs)) {
                logXSharedPreferencesError(prefs)
                return@executeWithLogging getDefaultRowConfigs()
            }

            // Reload configuration (ensure reading latest data)
            prefs.reload()

            XposedLogger.config("XSharedPreferences file is readable")
            XposedLogger.config(
                "File path: %s",
                PreferencesManager.getXSharedPreferencesFilePath(prefs)
            )

            // Read configuration JSON
            val configJson = prefs.getString(AppConfig.KEY_ROW_CONFIGS, null)

            if (configJson.isNullOrEmpty()) {
                XposedLogger.config("No '%s' key found in SharedPreferences", AppConfig.KEY_ROW_CONFIGS)
                XposedLogger.config("Using default fallback configuration")
                return@executeWithLogging getDefaultRowConfigs()
            }

            XposedLogger.config("Successfully read config JSON: %d chars", configJson.length)

            // Parse configuration
            val configs = parseConfigJson(configJson)

            XposedLogger.config("Successfully loaded %d row configurations", configs.size)

            configs
        } ?: getDefaultRowConfigs()
    }

    /**
     * Log XSharedPreferences error details
     */
    private fun logXSharedPreferencesError(prefs: XSharedPreferences) {
        XposedLogger.config("Cannot read XSharedPreferences file")
        XposedLogger.config(
            "File path: %s",
            PreferencesManager.getXSharedPreferencesFilePath(prefs)
        )
        XposedLogger.config(
            "File exists: %s",
            PreferencesManager.doesXSharedPreferencesExist(prefs)
        )
        XposedLogger.config(
            "Can read: %s",
            PreferencesManager.isXSharedPreferencesReadable(prefs)
        )
        XposedLogger.config("")
        XposedLogger.config("Possible reasons:")
        XposedLogger.config("1. Module UI never opened (config file not created yet)")
        XposedLogger.config("2. Module not using MODE_WORLD_READABLE in getSharedPreferences()")
        XposedLogger.config("3. xposedminversion < 93 in AndroidManifest.xml")
        XposedLogger.config("4. LSPosed version too old (need API 93+)")
        XposedLogger.config("")
        XposedLogger.config("Using default fallback configuration")
    }

    /**
     * Parse JSON configuration
     */
    private fun parseConfigJson(rowsJson: String): List<RowConfig> {
        return XposedLogger.executeWithLogging(
            "Parse JSON config",
            emptyList<RowConfig>()
        ) {
            if (rowsJson.isEmpty()) {
                XposedLogger.config("Config JSON is empty")
                return@executeWithLogging emptyList()
            }

            XposedLogger.config("Parsing JSON config: %d chars", rowsJson.length)

            // Parse JSON configuration
            val jsonObj = JSONObject(rowsJson)
            val version = jsonObj.getInt("version")
            val timestamp = jsonObj.getLong("timestamp")
            val rowsArray = jsonObj.getJSONArray("rows")

            // Add human-readable timestamp to verify latest config
            val timestampDate = java.text.SimpleDateFormat(
                AppConfig.TIMESTAMP_FORMAT,
                java.util.Locale.getDefault()
            ).format(java.util.Date(timestamp))

            XposedLogger.config(
                "Parsed JSON: version=%d, timestamp=%s (%s), rows=%d",
                version,
                timestamp,
                timestampDate,
                rowsArray.length()
            )

            val allRows = mutableListOf<RowConfig>()
            for (i in 0 until rowsArray.length()) {
                val rowObj = rowsArray.getJSONObject(i)
                val id = rowObj.getString("id")
                val enabled = rowObj.getBoolean("enabled")
                val offset = rowObj.getDouble("offset").toFloat()
                val itemsArray = rowObj.getJSONArray("items")

                val items = mutableListOf<Int>()
                for (j in 0 until itemsArray.length()) {
                    items.add(itemsArray.getInt(j))
                }

                // Read new fields (use defaults if not present)
                val horizontalMargin = if (rowObj.has("horizontalMargin")) {
                    rowObj.getDouble("horizontalMargin").toFloat()
                } else {
                    AppConfig.DEFAULT_HORIZONTAL_MARGIN_DP
                }

                val verticalSpacing = if (rowObj.has("verticalSpacing")) {
                    rowObj.getDouble("verticalSpacing").toFloat()
                } else {
                    AppConfig.DEFAULT_VERTICAL_SPACING_DP
                }

                val useFanTouchRegion = if (rowObj.has("useFanTouchRegion")) {
                    rowObj.getBoolean("useFanTouchRegion")
                } else {
                    AppConfig.DEFAULT_USE_FAN_TOUCH_REGION
                }

                val rowConfig = RowConfig(
                    id,
                    enabled,
                    offset,
                    items,
                    horizontalMargin,
                    verticalSpacing,
                    useFanTouchRegion
                )
                allRows.add(rowConfig)

                XposedLogger.config(
                    "Parsed row %d (%s): enabled=%s, offset=%.1f, margin=%.1f, spacing=%.1f, items=%s",
                    i + 1,
                    id,
                    enabled,
                    offset,
                    horizontalMargin,
                    verticalSpacing,
                    items.joinToString(",")
                )
            }

            allRows
        } ?: emptyList()
    }

    /**
     * Get default row configurations
     */
    private fun getDefaultRowConfigs(): List<RowConfig> {
        return listOf(
            RowConfig(
                "row_1",
                true,
                0f,
                AppConfig.DEFAULT_ROW_1_ICONS,
                AppConfig.DEFAULT_HORIZONTAL_MARGIN_DP,
                0f,
                AppConfig.DEFAULT_USE_FAN_TOUCH_REGION
            ),
            RowConfig(
                "row_2",
                true,
                1.4f,
                AppConfig.DEFAULT_ROW_2_ICONS,
                AppConfig.DEFAULT_HORIZONTAL_MARGIN_DP,
                AppConfig.DEFAULT_VERTICAL_SPACING_DP,
                AppConfig.DEFAULT_USE_FAN_TOUCH_REGION
            )
        )
    }

    /**
     * Load custom icon configuration
     * Uses LSPosed XSharedPreferences mechanism
     */
    private fun loadCustomIconConfig(itemId: Int): CustomIconConfig? {
        return XposedLogger.executeWithLogging(
            "Load custom icon config for id=$itemId",
            null
        ) {
            // Use XSharedPreferences to read configuration
            val prefs = PreferencesManager.getXSharedPreferences(
                AppConfig.MODULE_PACKAGE,
                AppConfig.PREFS_CONFIG_NAME
            )

            if (!PreferencesManager.isXSharedPreferencesReadable(prefs)) {
                XposedLogger.custom("Config file not accessible")
                return@executeWithLogging null
            }

            prefs.reload()

            // Read custom_icons configuration
            val customIconsJson = prefs.getString(AppConfig.KEY_CUSTOM_ICONS, null)

            if (customIconsJson.isNullOrEmpty()) {
                XposedLogger.custom("No custom icons configured")
                return@executeWithLogging null
            }

            // Parse JSON array
            val jsonArray = JSONArray(customIconsJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.getInt("id") == itemId) {
                    return@executeWithLogging CustomIconConfig.fromJson(obj)
                }
            }

            XposedLogger.custom("Custom icon ID %d not found in config", itemId)
            null
        }
    }

    /**
     * Find field in class hierarchy
     */
    private fun findFieldInHierarchy(startClass: Class<*>, fieldName: String): Field? {
        var currentClass: Class<*>? = startClass
        while (currentClass != null && currentClass != Any::class.java) {
            try {
                val field = currentClass.getDeclaredField(fieldName)
                field.isAccessible = true
                return field
            } catch (e: NoSuchFieldException) {
                currentClass = currentClass.superclass
            }
        }
        return null
    }

    /**
     * Derive layout reference values from first row positions
     */
    private fun getOrCreateLayoutReference(
        context: Context,
        firstRowPositions: List<PositionInfo>,
        screenWidth: Int
    ): LayoutReference {
        if (firstRowPositions.isEmpty()) {
            return LayoutReference.getDefault(screenWidth)
        }

        return LayoutReference(
            xMin = firstRowPositions.minOf { it.rect.left },
            xMax = firstRowPositions.maxOf { it.rect.right },
            baseY = firstRowPositions.map { it.rect.centerY() }.average().toFloat(),
            itemWidth = firstRowPositions.first().rect.width(),
            itemHeight = firstRowPositions.first().rect.height(),
            screenWidth = screenWidth,
            timestamp = System.currentTimeMillis()
        )
    }
}
