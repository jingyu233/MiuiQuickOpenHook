package com.jingyu233.miuiquickopenhook.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.jingyu233.miuiquickopenhook.data.CustomIconConfig
import com.jingyu233.miuiquickopenhook.data.IconType
import com.jingyu233.miuiquickopenhook.data.IntentAction
import com.jingyu233.miuiquickopenhook.data.LaunchType
import com.jingyu233.miuiquickopenhook.ui.theme.MiuiQuickOpenHookTheme
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class IconEditorActivity : ComponentActivity() {

    companion object {
        private const val TAG = "GXZW-MiuiQuickOpenHook-IconEditor"
        private const val PREFS_NAME = "miui_config"
        private const val CUSTOM_ICONS_KEY = "custom_icons"
    }

    private lateinit var prefs: android.content.SharedPreferences
    private var editMode = false
    private var originalConfig: CustomIconConfig? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        prefs = try {
            getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot create MODE_WORLD_READABLE SharedPreferences, falling back to MODE_PRIVATE")
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        }

        val mode = intent.getStringExtra("mode") ?: "add"
        editMode = mode == "edit"

        originalConfig = if (editMode) {
            // 从 SharedPreferences 读取配置，避免通过 Intent 传递大数据
            val iconId = intent.getIntExtra("icon_id", -1)
            if (iconId != -1) {
                try {
                    loadConfigById(iconId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load config for ID $iconId: ${e.message}")
                    null
                }
            } else null
        } else null

        setContent {
            MiuiQuickOpenHookTheme {
                IconEditorScreen(
                    editMode = editMode,
                    initialConfig = originalConfig,
                    onBackPressed = { finish() },
                    onSave = { config -> saveConfig(config) }
                )
            }
        }
    }

    private fun loadConfigById(iconId: Int): CustomIconConfig? {
        return try {
            val iconsJson = prefs.getString(CUSTOM_ICONS_KEY, "[]") ?: "[]"
            val jsonArray = JSONArray(iconsJson)

            for (i in 0 until jsonArray.length()) {
                val config = CustomIconConfig.fromJson(jsonArray.getJSONObject(i))
                if (config.id == iconId) {
                    Log.d(TAG, "Found config for ID $iconId")
                    return config
                }
            }

            Log.w(TAG, "Config not found for ID $iconId")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load config by ID: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun saveConfig(config: CustomIconConfig): Boolean {
        return try {
            Log.d(TAG, "开始保存图标配置")
            Log.d(TAG, "  - 编辑模式: $editMode")
            Log.d(TAG, "  - 图标ID: ${config.id}")
            Log.d(TAG, "  - 标题: ${config.title}")
            Log.d(TAG, "  - 副标题: ${config.subtitle}")
            Log.d(TAG, "  - 图标类型: ${config.iconType.value}")
            Log.d(TAG, "  - 图标数据: ${config.iconData?.take(100)}...") // 只显示前100字符
            Log.d(TAG, "  - 包名: ${config.packageName}")
            Log.d(TAG, "  - Activity: ${config.activityName}")

            val iconsJson = prefs.getString(CUSTOM_ICONS_KEY, "[]") ?: "[]"
            Log.d(TAG, "读取现有配置 JSON 长度: ${iconsJson.length}")

            val jsonArray = JSONArray(iconsJson)
            val iconsList = mutableListOf<CustomIconConfig>()

            // Load existing icons
            for (i in 0 until jsonArray.length()) {
                iconsList.add(CustomIconConfig.fromJson(jsonArray.getJSONObject(i)))
            }
            Log.d(TAG, "已加载 ${iconsList.size} 个现有图标")

            if (editMode) {
                // Update existing icon
                val index = iconsList.indexOfFirst { it.id == config.id }
                if (index != -1) {
                    Log.d(TAG, "更新现有图标，索引: $index")
                    iconsList[index] = config
                } else {
                    Log.w(TAG, "图标ID ${config.id} 未找到，作为新图标添加")
                    iconsList.add(config)
                }
            } else {
                // Check for duplicate ID
                if (iconsList.any { it.id == config.id }) {
                    Log.e(TAG, "图标ID ${config.id} 已存在！")
                    return false
                }
                Log.d(TAG, "添加新图标")
                iconsList.add(config)
            }

            // Save back to SharedPreferences
            val newJsonArray = JSONArray()
            iconsList.forEach { newJsonArray.put(it.toJson()) }

            val finalJson = newJsonArray.toString()
            Log.d(TAG, "准备保存 ${iconsList.size} 个图标，JSON 长度: ${finalJson.length}")

            prefs.edit()
                .putString(CUSTOM_ICONS_KEY, finalJson)
                .apply()

            // 验证保存
            val savedValue = prefs.getString(CUSTOM_ICONS_KEY, null)
            if (savedValue == finalJson) {
                Log.i(TAG, "验证成功：配置已正确写入 SharedPreferences")
            } else {
                Log.w(TAG, "验证警告：读取的值与写入的值不完全一致")
            }

            Log.i(TAG, "成功保存图标配置: ID ${config.id}")
            setResult(Activity.RESULT_OK)
            finish()
            true
        } catch (e: Exception) {
            Log.e(TAG, "保存配置失败: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconEditorScreen(
    editMode: Boolean,
    initialConfig: CustomIconConfig?,
    onBackPressed: () -> Unit,
    onSave: (CustomIconConfig) -> Boolean
) {
    // Form state
    var iconId by remember { mutableStateOf(initialConfig?.id?.toString() ?: "") }
    var title by remember { mutableStateOf(initialConfig?.title ?: "") }
    var subtitle by remember { mutableStateOf(initialConfig?.subtitle ?: "") }
    var packageName by remember { mutableStateOf(initialConfig?.packageName ?: "") }
    var activityName by remember { mutableStateOf(initialConfig?.activityName ?: "") }
    var dataUri by remember { mutableStateOf(initialConfig?.data ?: "") }
    var customAction by remember { mutableStateOf(initialConfig?.customAction ?: "") }

    var launchType by remember { mutableStateOf(initialConfig?.launchType ?: LaunchType.ACTIVITY) }
    var intentAction by remember { mutableStateOf(initialConfig?.action ?: IntentAction.MAIN) }
    var useNewTask by remember { mutableStateOf(initialConfig?.useNewTask ?: true) }
    var useRoot by remember { mutableStateOf(initialConfig?.useRoot ?: false) }

    // Extras management
    var extras by remember { mutableStateOf(initialConfig?.extras ?: emptyMap()) }
    var showAddExtraDialog by remember { mutableStateOf(false) }

    // Validation errors
    var idError by remember { mutableStateOf<String?>(null) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var packageError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }

    // Icon configuration state
    var iconType by remember { mutableStateOf(initialConfig?.iconType ?: IconType.APP_ICON) }
    var iconData by remember { mutableStateOf(initialConfig?.iconData) }
    var iconPreviewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // Get context early for use in callbacks
    val context = androidx.compose.ui.platform.LocalContext.current

    // 判断是否是系统预置图标(1001-1008),这类图标只能修改标题、副标题和图标
    val isSystemPresetIcon = remember(iconId) {
        val id = iconId.toIntOrNull()
        id != null && id in 1001..1008
    }

    // App picker launcher
    val appPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val selectedPackageName = data.getStringExtra(AppPickerActivity.EXTRA_PACKAGE_NAME)
                val selectedActivity = data.getStringExtra(AppPickerActivity.EXTRA_ACTIVITY_NAME)
                val selectedLabel = data.getStringExtra(AppPickerActivity.EXTRA_APP_LABEL)

                if (selectedPackageName != null) {
                    // Update icon to use app icon
                    iconType = IconType.APP_ICON
                    iconData = selectedPackageName

                    // Auto-fill form fields if empty
                    if (packageName.isBlank()) {
                        packageName = selectedPackageName
                    }
                    if (activityName.isBlank() && selectedActivity != null) {
                        activityName = selectedActivity
                    }
                    if (title.isBlank() && selectedLabel != null) {
                        title = selectedLabel
                    }
                }
            }
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                // Scale down bitmap if too large
                val maxSize = 512
                val scaledBitmap = if (bitmap.width > maxSize || bitmap.height > maxSize) {
                    val scale = maxSize.toFloat() / kotlin.math.max(bitmap.width, bitmap.height)
                    val newWidth = (bitmap.width * scale).toInt()
                    val newHeight = (bitmap.height * scale).toInt()
                    android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                } else {
                    bitmap
                }

                // Convert to Base64
                val byteArrayOutputStream = ByteArrayOutputStream()
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

                // Update icon to use custom image
                iconType = IconType.CUSTOM_IMAGE
                iconData = base64String
                iconPreviewBitmap = scaledBitmap
            } catch (e: Exception) {
                Log.e("IconEditorActivity", "Failed to load image: ${e.message}")
            }
        }
    }

    // Load icon preview for existing configs
    LaunchedEffect(initialConfig) {
        initialConfig?.let { config ->
            try {
                when (config.iconType) {
                    IconType.APP_ICON -> {
                        config.iconData?.let { packageName ->
                            try {
                                val pm = context.packageManager
                                val icon = pm.getApplicationIcon(packageName)
                                iconPreviewBitmap = icon.toBitmap(512, 512)
                            } catch (e: Exception) {
                                Log.e("IconEditorActivity", "Failed to load app icon: ${e.message}")
                            }
                        }
                    }
                    IconType.CUSTOM_IMAGE -> {
                        config.iconData?.let { base64String ->
                            try {
                                val decodedBytes = Base64.decode(base64String, Base64.NO_WRAP)
                                iconPreviewBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            } catch (e: Exception) {
                                Log.e("IconEditorActivity", "Failed to decode Base64 image: ${e.message}")
                            }
                        }
                    }
                    IconType.SYSTEM_RES -> {
                        // System resources will be shown in the actual UI, no preview needed here
                    }
                }
            } catch (e: Exception) {
                Log.e("IconEditorActivity", "Failed to load icon preview: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editMode) "编辑图标" else "添加图标") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onBackPressed) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            // Validate inputs
                            idError = null
                            titleError = null
                            packageError = null
                            saveError = null

                            val id = iconId.toIntOrNull()
                            when {
                                id == null -> idError = "图标ID必须是数字"
                                id < 1001 -> idError = "图标ID不能小于1001"
                                id in 1009..2000 -> idError = "图标ID范围: 1001-1008 或 2001-9999"
                                id > 9999 -> idError = "图标ID不能大于9999"
                                title.isBlank() -> titleError = "标题不能为空"
                                // 对于2001-9999,包名是必填的;对于1001-1008,包名可选(因为可能只改标题和图标)
                                !isSystemPresetIcon && packageName.isBlank() -> packageError = "包名不能为空"
                                else -> {
                                    val config = CustomIconConfig(
                                        id = id,
                                        enabled = true,
                                        title = title.trim(),
                                        subtitle = subtitle.trim(),
                                        iconType = iconType,
                                        iconData = iconData,
                                        launchType = launchType,
                                        // 对于系统预置图标,如果包名为空,使用默认值
                                        packageName = if (packageName.isBlank()) "com.android.systemui" else packageName.trim(),
                                        activityName = activityName.trim().takeIf { it.isNotEmpty() },
                                        data = dataUri.trim().takeIf { it.isNotEmpty() },
                                        extras = extras,
                                        action = intentAction,
                                        customAction = customAction.trim().takeIf { it.isNotEmpty() },
                                        useNewTask = useNewTask,
                                        useRoot = useRoot
                                    )

                                    if (!onSave(config)) {
                                        saveError = "保存失败：图标ID ${id} 已存在"
                                    }
                                }
                            }
                        }
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error message
            if (saveError != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = saveError!!,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Basic Information Section
            Text(
                text = "基本信息",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // 系统预置图标提示
            if (isSystemPresetIcon) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "📌 系统预置图标 (1001-1008)\n仅可修改：标题、副标题、图标\nIntent配置字段将被禁用",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            OutlinedTextField(
                value = iconId,
                onValueChange = {
                    // 只允许输入数字
                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                        iconId = it
                        idError = null
                    }
                },
                label = { Text("图标ID") },
                placeholder = { Text("1001-1008 或 2001-9999") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !editMode,
                isError = idError != null,
                supportingText = idError?.let { { Text(it) } },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = null
                },
                label = { Text("标题") },
                placeholder = { Text("例如：微信付款码") },
                modifier = Modifier.fillMaxWidth(),
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it) } }
            )

            OutlinedTextField(
                value = subtitle,
                onValueChange = { subtitle = it },
                label = { Text("副标题(可选)") },
                placeholder = { Text("例如：快速支付") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            // Icon Selection Section
            Text(
                text = "图标设置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Icon preview
            if (iconPreviewBitmap != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = iconPreviewBitmap!!.asImageBitmap(),
                            contentDescription = "图标预览",
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            }

            // Icon type indicator
            Text(
                text = when (iconType) {
                    IconType.SYSTEM_RES -> "当前图标类型：系统资源"
                    IconType.APP_ICON -> "当前图标类型：应用图标 ${iconData?.let { "($it)" } ?: ""}"
                    IconType.CUSTOM_IMAGE -> "当前图标类型：自定义图片"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Select from app
                OutlinedButton(
                    onClick = {
                        val intent = Intent(context, AppPickerActivity::class.java)
                        appPickerLauncher.launch(intent)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("选择应用")
                }

                // Select from gallery
                OutlinedButton(
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("选择图片")
                }
            }

            HorizontalDivider()

            // Intent Configuration Section
            Text(
                text = "Intent 配置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = packageName,
                onValueChange = {
                    packageName = it
                    packageError = null
                },
                label = { Text("包名") },
                placeholder = { Text("com.tencent.mm") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSystemPresetIcon,
                isError = packageError != null,
                supportingText = packageError?.let { { Text(it) } }
            )

            OutlinedTextField(
                value = activityName,
                onValueChange = { activityName = it },
                label = { Text("Activity/组件名（可选）") },
                placeholder = { Text("com.tencent.mm.plugin.offline.ui.WalletOfflineCoinPurseUI") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSystemPresetIcon
            )

            // Launch Type Dropdown
            var launchTypeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = launchTypeExpanded,
                onExpandedChange = { if (!isSystemPresetIcon) launchTypeExpanded = it }
            ) {
                OutlinedTextField(
                    value = when (launchType) {
                        LaunchType.ACTIVITY -> "Activity"
                        LaunchType.SERVICE -> "Service"
                        LaunchType.BROADCAST -> "Broadcast"
                    },
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isSystemPresetIcon,
                    label = { Text("启动类型") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = launchTypeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = launchTypeExpanded,
                    onDismissRequest = { launchTypeExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Activity") },
                        onClick = {
                            launchType = LaunchType.ACTIVITY
                            launchTypeExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Service") },
                        onClick = {
                            launchType = LaunchType.SERVICE
                            launchTypeExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Broadcast") },
                        onClick = {
                            launchType = LaunchType.BROADCAST
                            launchTypeExpanded = false
                        }
                    )
                }
            }

            HorizontalDivider()

            // Advanced Settings Section
            Text(
                text = "高级设置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Intent Action Dropdown
            var actionExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = actionExpanded,
                onExpandedChange = { if (!isSystemPresetIcon) actionExpanded = it }
            ) {
                OutlinedTextField(
                    value = when (intentAction) {
                        IntentAction.MAIN -> "MAIN"
                        IntentAction.VIEW -> "VIEW"
                        IntentAction.SEND -> "SEND"
                        IntentAction.CUSTOM -> "自定义"
                    },
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isSystemPresetIcon,
                    label = { Text("Intent Action") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = actionExpanded,
                    onDismissRequest = { actionExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("MAIN (android.intent.action.MAIN)") },
                        onClick = {
                            intentAction = IntentAction.MAIN
                            actionExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("VIEW (android.intent.action.VIEW)") },
                        onClick = {
                            intentAction = IntentAction.VIEW
                            actionExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("SEND (android.intent.action.SEND)") },
                        onClick = {
                            intentAction = IntentAction.SEND
                            actionExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("自定义") },
                        onClick = {
                            intentAction = IntentAction.CUSTOM
                            actionExpanded = false
                        }
                    )
                }
            }

            if (intentAction == IntentAction.CUSTOM) {
                OutlinedTextField(
                    value = customAction,
                    onValueChange = { customAction = it },
                    label = { Text("自定义 Action") },
                    placeholder = { Text("com.example.CUSTOM_ACTION") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSystemPresetIcon
                )
            }

            OutlinedTextField(
                value = dataUri,
                onValueChange = { dataUri = it },
                label = { Text("Data URI（可选）") },
                placeholder = { Text("https://example.com 或 content://...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSystemPresetIcon
            )

            HorizontalDivider()

            // Extras Section
            Text(
                text = "Intent Extras",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (extras.isEmpty()) {
                Text(
                    text = "暂无额外参数",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                extras.forEach { (key, value) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Key: $key",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Value: $value",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            IconButton(
                                onClick = {
                                    extras = extras.filterKeys { it != key }
                                },
                                enabled = !isSystemPresetIcon
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "删除")
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { showAddExtraDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSystemPresetIcon
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加 Extra")
            }

            HorizontalDivider()

            // Flags Section
            Text(
                text = "启动标志",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FLAG_ACTIVITY_NEW_TASK",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "在新任务中启动",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useNewTask,
                    onCheckedChange = { useNewTask = it },
                    enabled = !isSystemPresetIcon
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "使用 Root 模式",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "仅识别包名和Activity，忽略其他参数",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useRoot,
                    onCheckedChange = { useRoot = it },
                    enabled = !isSystemPresetIcon
                )
            }

            if (useRoot) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = "⚠️ Root 模式需要设备已获取 Root 权限，并且模块具有 Root 访问权限。启用后将忽略 Data URI、Extras、Action 等高级参数。",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }

    // Add Extra Dialog
    if (showAddExtraDialog) {
        var extraKey by remember { mutableStateOf("") }
        var extraValue by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddExtraDialog = false },
            title = { Text("添加 Extra 参数") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = extraKey,
                        onValueChange = { extraKey = it },
                        label = { Text("Key") },
                        placeholder = { Text("例如：key_name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = extraValue,
                        onValueChange = { extraValue = it },
                        label = { Text("Value") },
                        placeholder = { Text("例如：value_content") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (extraKey.isNotBlank() && extraValue.isNotBlank()) {
                            extras = extras + (extraKey.trim() to extraValue.trim())
                            showAddExtraDialog = false
                        }
                    },
                    enabled = extraKey.isNotBlank() && extraValue.isNotBlank()
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExtraDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
