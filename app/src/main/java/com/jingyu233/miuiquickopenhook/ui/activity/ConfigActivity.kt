package com.jingyu233.miuiquickopenhook.ui.activity

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jingyu233.miuiquickopenhook.data.RowConfig
import com.jingyu233.miuiquickopenhook.ui.theme.MiuiQuickOpenHookTheme
import org.json.JSONArray
import org.json.JSONObject

/**
 * 图标名称获取工具
 */
object IconNameHelper {
    /**
     * 从 SharedPreferences 加载自定义图标配置并获取图标名称
     */
    fun getIconName(prefs: android.content.SharedPreferences, id: Int): String {
        return try {
            val customIconsJson = prefs.getString("custom_icons", "[]") ?: "[]"
            val jsonArray = JSONArray(customIconsJson)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.getInt("id") == id) {
                    return obj.getString("title")
                }
            }

            // 默认图标名称
            getDefaultIconName(id)
        } catch (e: Exception) {
            getDefaultIconName(id)
        }
    }

    private fun getDefaultIconName(id: Int): String {
        return when (id) {
            1001 -> "微信付款码"
            1002 -> "微信扫一扫"
            1003 -> "小爱同学"
            1004 -> "支付宝付款"
            1005 -> "支付宝扫一扫"
            1006 -> "二维码"
            1007 -> "搜索"
            1008 -> "添加事件"
            else -> "自定义图标"
        }
    }
}

/**
 * 配置管理界面
 */
class ConfigActivity : ComponentActivity() {

    companion object {
        private const val TAG = "GXZW-MiuiQuickOpenHook-Config"
        private const val PREFS_NAME = "miui_config"
        private const val ROW_CONFIGS_KEY = "row_configs"
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 使用 MODE_WORLD_READABLE 以便 LSPosed 的 New XSharedPreferences 机制可以共享给 SystemUI
        // LSPosed 会自动 hook ContextImpl，把这个 preference 放到可共享的位置
        @Suppress("DEPRECATION")
        prefs = try {
            getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
        } catch (e: SecurityException) {
            Log.e(TAG, "无法创建 MODE_WORLD_READABLE 的 SharedPreferences，LSPosed 可能未正确加载")
            Log.e(TAG, "  回退到 MODE_PRIVATE")
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        }

        Log.i(TAG, "SharedPreferences 已初始化: $PREFS_NAME (MODE_WORLD_READABLE)")

        setContent {
            MiuiQuickOpenHookTheme {
                ConfigScreen(
                    prefs = prefs,
                    onBackPressed = { finish() },
                    onSaveConfig = { saveConfigs(it) },
                    loadConfigs = { loadConfigs() }
                )
            }
        }
    }

    private fun loadConfigs(): List<RowConfig> {
        try {
            Log.d(TAG, "开始加载配置")
            Log.d(TAG, "  - PREFS_NAME: $PREFS_NAME")
            Log.d(TAG, "  - ROW_CONFIGS_KEY: $ROW_CONFIGS_KEY")

            val configJson = prefs.getString(ROW_CONFIGS_KEY, null)

            if (configJson.isNullOrEmpty()) {
                Log.w(TAG, "配置文件为空，使用默认配置")
                // 返回默认配置
                return listOf(
                    RowConfig("row_1", true, 0f, listOf(1001, 1002, 1003, 1004, 1005)),
                    RowConfig("row_2", true, 1.4f, listOf(1006, 1007, 1008, 1003))
                )
            }

            Log.d(TAG, "读取到配置 JSON，长度: ${configJson.length} 字符")
            Log.d(TAG, "JSON 内容: $configJson")

            val jsonObj = JSONObject(configJson)
            val version = jsonObj.optInt("version", 0)
            val timestamp = jsonObj.optLong("timestamp", 0)
            Log.d(TAG, "配置版本: $version, 时间戳: $timestamp")

            val rowsArray = jsonObj.getJSONArray("rows")
            Log.d(TAG, "配置行数: ${rowsArray.length()}")

            val configs = mutableListOf<RowConfig>()

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

                val horizontalMargin = if (rowObj.has("horizontalMargin")) {
                    rowObj.getDouble("horizontalMargin").toFloat()
                } else {
                    60f
                }

                val verticalSpacing = if (rowObj.has("verticalSpacing")) {
                    rowObj.getDouble("verticalSpacing").toFloat()
                } else {
                    140f
                }

                val useFanTouchRegion = if (rowObj.has("useFanTouchRegion")) {
                    rowObj.getBoolean("useFanTouchRegion")
                } else {
                    true
                }

                val config = RowConfig(id, enabled, offset, items, horizontalMargin, verticalSpacing, useFanTouchRegion)
                configs.add(config)

                Log.d(TAG, "加载配置 #${i + 1}: ${config.id}")
                Log.d(TAG, "  - enabled: ${config.enabled}")
                Log.d(TAG, "  - items: ${config.itemIds.joinToString(",")}")
                Log.d(TAG, "  - horizontalMargin: ${config.horizontalMargin}")
                Log.d(TAG, "  - verticalSpacing: ${config.verticalSpacing}")
            }

            Log.i(TAG, "成功加载 ${configs.size} 个配置")
            return configs
        } catch (e: Exception) {
            Log.e(TAG, "加载配置时发生异常: ${e.message}", e)
            e.printStackTrace()
            // 出错时返回默认配置
            return listOf(
                RowConfig("row_1", true, 0f, listOf(1001, 1002, 1003, 1004, 1005)),
                RowConfig("row_2", true, 1.4f, listOf(1006, 1007, 1008, 1003))
            )
        }
    }

    private fun saveConfigs(configs: List<RowConfig>) {
        try {
            Log.d(TAG, "开始保存配置，配置数量: ${configs.size}")

            val jsonObj = JSONObject()
            jsonObj.put("version", 1)
            jsonObj.put("timestamp", System.currentTimeMillis())

            val rowsArray = JSONArray()
            configs.forEachIndexed { index, config ->
                Log.d(TAG, "处理第 ${index + 1} 个配置: ${config.id}")
                Log.d(TAG, "  - enabled: ${config.enabled}")
                Log.d(TAG, "  - itemIds: ${config.itemIds.joinToString(",")}")
                Log.d(TAG, "  - horizontalMargin: ${config.horizontalMargin}")
                Log.d(TAG, "  - verticalSpacing: ${config.verticalSpacing}")

                val rowObj = JSONObject()
                rowObj.put("id", config.id)
                rowObj.put("enabled", config.enabled)
                rowObj.put("offset", config.verticalOffset)
                rowObj.put("items", JSONArray(config.itemIds))
                rowObj.put("horizontalMargin", config.horizontalMargin)
                rowObj.put("verticalSpacing", config.verticalSpacing)
                rowObj.put("useFanTouchRegion", config.useFanTouchRegion)
                rowsArray.put(rowObj)
            }
            jsonObj.put("rows", rowsArray)

            val jsonString = jsonObj.toString()
            Log.d(TAG, "生成的 JSON 长度: ${jsonString.length} 字符")
            Log.d(TAG, "JSON 内容: $jsonString")

            Log.d(TAG, "开始写入 SharedPreferences...")
            Log.d(TAG, "  - PREFS_NAME: $PREFS_NAME")
            Log.d(TAG, "  - ROW_CONFIGS_KEY: $ROW_CONFIGS_KEY")

            val editor = prefs.edit()
            editor.putString(ROW_CONFIGS_KEY, jsonString)
            val success = editor.commit()  // 使用 commit() 而非 apply() 以便立即知道结果

            if (success) {
                Log.i(TAG, "配置保存成功！")

                // 验证保存是否成功
                val savedValue = prefs.getString(ROW_CONFIGS_KEY, null)
                if (savedValue != null && savedValue == jsonString) {
                    Log.i(TAG, "验证成功：配置已正确写入 SharedPreferences")
                    Toast.makeText(this, "配置保存成功！", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "验证失败：读取的配置与保存的不一致")
                    Log.e(TAG, "  - 保存的长度: ${jsonString.length}")
                    Log.e(TAG, "  - 读取的长度: ${savedValue?.length ?: 0}")
                    Toast.makeText(this, "配置保存可能失败，请检查日志", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.e(TAG, "SharedPreferences commit() 返回 false")
                Toast.makeText(this, "保存失败！", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e(TAG, "保存配置时发生异常: ${e.message}", e)
            e.printStackTrace()
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    prefs: SharedPreferences,
    onBackPressed: () -> Unit,
    onSaveConfig: (List<RowConfig>) -> Unit,
    loadConfigs: () -> List<RowConfig>
) {
    var configs by remember { mutableStateOf(loadConfigs()) }
    var isHelpExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MIUI快速打开配置") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    configs = configs + RowConfig(
                        "row_${configs.size + 1}",
                        true,
                        1.4f,
                        emptyList()
                    )
                },
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 可折叠的配置说明
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { isHelpExpanded = !isHelpExpanded }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "配置说明",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            imageVector = if (isHelpExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isHelpExpanded) "收起" else "展开"
                        )
                    }

                    if (isHelpExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• row_1: 第一排使用系统原始扇形布局\n" +
                                   "• row_2及以后: 使用自定义布局\n" +
                                   "• 水平边距: 左右两侧的留白距离(dp)\n" +
                                   "• 距离底部: 距离屏幕底部的距离(dp)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(configs) { config ->
                    ConfigItemCard(
                        prefs = prefs,
                        config = config,
                        isFirstRow = config.id == "row_1",
                        onEnabledChange = { enabled ->
                            configs = configs.map {
                                if (it.id == config.id) it.copy(enabled = enabled) else it
                            }
                        },
                        onHorizontalMarginChange = { margin ->
                            configs = configs.map {
                                if (it.id == config.id) it.copy(horizontalMargin = margin) else it
                            }
                        },
                        onVerticalSpacingChange = { spacing ->
                            configs = configs.map {
                                if (it.id == config.id) it.copy(verticalSpacing = spacing) else it
                            }
                        },
                        onFanTouchRegionChange = { useFan ->
                            configs = configs.map {
                                if (it.id == config.id) it.copy(useFanTouchRegion = useFan) else it
                            }
                        },
                        onDelete = {
                            configs = configs.filter { it.id != config.id }
                        },
                        onAddItem = {
                            val newItems = config.itemIds.toMutableList()
                            newItems.add(1001)
                            configs = configs.map {
                                if (it.id == config.id) it.copy(itemIds = newItems) else it
                            }
                        },
                        onRemoveItem = { index ->
                            val newItems = config.itemIds.toMutableList()
                            if (index in newItems.indices) {
                                newItems.removeAt(index)
                                configs = configs.map {
                                    if (it.id == config.id) it.copy(itemIds = newItems) else it
                                }
                            }
                        },
                        onItemIdChange = { index, newId ->
                            val newItems = config.itemIds.toMutableList()
                            if (index in newItems.indices) {
                                newItems[index] = newId
                                configs = configs.map {
                                    if (it.id == config.id) it.copy(itemIds = newItems) else it
                                }
                            }
                        },
                        onMoveItemUp = { index ->
                            if (index > 0) {
                                val newItems = config.itemIds.toMutableList()
                                val temp = newItems[index]
                                newItems[index] = newItems[index - 1]
                                newItems[index - 1] = temp
                                configs = configs.map {
                                    if (it.id == config.id) it.copy(itemIds = newItems) else it
                                }
                            }
                        },
                        onMoveItemDown = { index ->
                            val newItems = config.itemIds.toMutableList()
                            if (index < newItems.size - 1) {
                                val temp = newItems[index]
                                newItems[index] = newItems[index + 1]
                                newItems[index + 1] = temp
                                configs = configs.map {
                                    if (it.id == config.id) it.copy(itemIds = newItems) else it
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSaveConfig(configs) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存配置")
            }
        }
    }
}

@Composable
fun ConfigItemCard(
    prefs: SharedPreferences,
    config: RowConfig,
    isFirstRow: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onHorizontalMarginChange: (Float) -> Unit,
    onVerticalSpacingChange: (Float) -> Unit,
    onFanTouchRegionChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onItemIdChange: (Int, Int) -> Unit,
    onMoveItemUp: (Int) -> Unit,
    onMoveItemDown: (Int) -> Unit
) {
    // 获取屏幕尺寸
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp

    // 水平边距最大值为屏幕宽度的一半
    val maxHorizontalMargin = (screenWidthDp / 2).toFloat()
    // 距离底部最大值为屏幕高度
    val maxVerticalSpacing = screenHeightDp.toFloat()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = config.id,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "启用")
                    Switch(
                        checked = config.enabled,
                        onCheckedChange = onEnabledChange
                    )
                    // Row1 不能删除，其他行可以删除
                    if (!isFirstRow) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 只有非第一排才显示布局配置
            if (config.id != "row_1") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "水平边距:",
                        modifier = Modifier.width(100.dp)
                    )
                    Slider(
                        value = config.horizontalMargin,
                        onValueChange = { onHorizontalMarginChange(it) },
                        valueRange = 0f..maxHorizontalMargin,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${config.horizontalMargin.toInt()}dp",
                        modifier = Modifier.width(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "距离底部:",
                        modifier = Modifier.width(100.dp)
                    )
                    Slider(
                        value = config.verticalSpacing,
                        onValueChange = { onVerticalSpacingChange(it) },
                        valueRange = 50f..maxVerticalSpacing,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${config.verticalSpacing.toInt()}dp",
                        modifier = Modifier.width(60.dp)
                    )
                }
            } else {
                // 第一排可以配置是否使用扇形触摸区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "使用扇形触摸区域")
                    Switch(
                        checked = config.useFanTouchRegion,
                        onCheckedChange = onFanTouchRegionChange
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "图标配置:",
                style = MaterialTheme.typography.bodyMedium
            )

            config.itemIds.forEachIndexed { index, itemId ->
                var showIconPicker by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${index + 1}:",
                        modifier = Modifier.width(50.dp)
                    )
                    OutlinedButton(
                        onClick = { showIconPicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${IconNameHelper.getIconName(prefs, itemId)} ($itemId)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // 上移按钮
                    IconButton(
                        onClick = { onMoveItemUp(index) },
                        enabled = index > 0
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移")
                    }
                    // 下移按钮
                    IconButton(
                        onClick = { onMoveItemDown(index) },
                        enabled = index < config.itemIds.size - 1
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移")
                    }
                    // 删除按钮
                    IconButton(onClick = {
                        onRemoveItem(index)
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }

                // 图标选择器对话框
                if (showIconPicker) {
                    IconPickerDialog(
                        prefs = prefs,
                        currentIconId = itemId,
                        onDismiss = { showIconPicker = false },
                        onConfirm = { newId ->
                            onItemIdChange(index, newId)
                            showIconPicker = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 如果是 Row1，限制最多5个图标
            val canAddMore = if (isFirstRow) config.itemIds.size < 5 else true

            Button(
                onClick = onAddItem,
                enabled = canAddMore,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isFirstRow && !canAddMore) {
                        "Row1最多5个图标"
                    } else {
                        "添加图标"
                    }
                )
            }
        }
    }
}

/**
 * 图标选择器对话框
 */
@Composable
fun IconPickerDialog(
    prefs: SharedPreferences,
    currentIconId: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // 加载所有可用的图
    val availableIcons = remember {
        buildList {
            // 系统预设图标 (1001-1008)
            addAll((1001..1008).map { id ->
                id to IconNameHelper.getIconName(prefs, id)
            })

            // 自定义图标 (2001-9999)
            try {
                val customIconsJson = prefs.getString("custom_icons", "[]") ?: "[]"
                val jsonArray = JSONArray(customIconsJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.getInt("id")
                    val title = obj.getString("title")
                    add(id to title)
                }
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }

    // 过滤图标
    val filteredIcons = remember(searchQuery, availableIcons) {
        if (searchQuery.isBlank()) {
            availableIcons
        } else {
            availableIcons.filter { (id, name) ->
                name.contains(searchQuery, ignoreCase = true) ||
                id.toString().contains(searchQuery)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择图标") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text("搜索图标名称或ID") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "清除"
                                )
                            }
                        }
                    }
                )

                // 图标列表
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredIcons.size) { index ->
                        val (id, name) = filteredIcons[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onConfirm(id)
                                    onDismiss()
                                }
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentIconId == id,
                                onClick = {
                                    onConfirm(id)
                                    onDismiss()
                                },
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "ID: $id",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (index < filteredIcons.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
