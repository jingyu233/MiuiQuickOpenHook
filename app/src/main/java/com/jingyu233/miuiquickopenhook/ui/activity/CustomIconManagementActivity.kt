package com.jingyu233.miuiquickopenhook.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jingyu233.miuiquickopenhook.data.CustomIconConfig
import com.jingyu233.miuiquickopenhook.data.LaunchType
import com.jingyu233.miuiquickopenhook.ui.theme.MiuiQuickOpenHookTheme
import org.json.JSONArray

class CustomIconManagementActivity : ComponentActivity() {

    companion object {
        private const val TAG = "GXZW-MiuiQuickOpenHook-CustomIconManagement"
        private const val PREFS_NAME = "miui_config"
        private const val CUSTOM_ICONS_KEY = "custom_icons"
        private const val REQUEST_CODE_EDIT = 1001
    }

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        prefs = try {
            getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot create MODE_WORLD_READABLE SharedPreferences, falling back to MODE_PRIVATE")
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        }

        setContent {
            MiuiQuickOpenHookTheme {
                CustomIconManagementScreen(
                    onBackPressed = { finish() },
                    onAddIcon = { launchIconEditor(null) },
                    onEditIcon = { config -> launchIconEditor(config) },
                    loadIcons = { loadCustomIcons() },
                    saveIcons = { icons -> saveCustomIcons(icons) }
                )
            }
        }
    }

    private fun launchIconEditor(config: CustomIconConfig?) {
        val intent = Intent(this, IconEditorActivity::class.java)
        if (config != null) {
            intent.putExtra("mode", "edit")
            // 只传递 ID，避免 TransactionTooLargeException
            intent.putExtra("icon_id", config.id)
        } else {
            intent.putExtra("mode", "add")
        }
        startActivityForResult(intent, REQUEST_CODE_EDIT)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_EDIT && resultCode == RESULT_OK) {
            // Refresh the UI by recomposing
            recreate()
        }
    }

    private fun loadCustomIcons(): List<CustomIconConfig> {
        return try {
            val json = prefs.getString(CUSTOM_ICONS_KEY, "[]") ?: "[]"
            Log.d(TAG, "加载自定义图标配置，JSON长度: ${json.length}")
            Log.d(TAG, "JSON内容: $json")

            val jsonArray = JSONArray(json)
            val icons = mutableListOf<CustomIconConfig>()

            for (i in 0 until jsonArray.length()) {
                try {
                    val obj = jsonArray.getJSONObject(i)
                    Log.d(TAG, "解析图标 #$i: ${obj.toString()}")
                    icons.add(CustomIconConfig.fromJson(obj))
                } catch (e: Exception) {
                    Log.e(TAG, "解析图标 #$i 失败: ${e.message}")
                    e.printStackTrace()
                }
            }

            Log.i(TAG, "Loaded ${icons.size} custom icons")
            icons
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load custom icons: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveCustomIcons(icons: List<CustomIconConfig>) {
        try {
            val jsonArray = JSONArray()
            icons.forEach { jsonArray.put(it.toJson()) }

            prefs.edit()
                .putString(CUSTOM_ICONS_KEY, jsonArray.toString())
                .apply()

            Log.i(TAG, "Saved ${icons.size} custom icons")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom icons: ${e.message}")
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomIconManagementScreen(
    onBackPressed: () -> Unit,
    onAddIcon: () -> Unit,
    onEditIcon: (CustomIconConfig) -> Unit,
    loadIcons: () -> List<CustomIconConfig>,
    saveIcons: (List<CustomIconConfig>) -> Unit
) {
    var icons by remember { mutableStateOf(loadIcons()) }
    var showDeleteDialog by remember { mutableStateOf<CustomIconConfig?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自定义图标管理") },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddIcon,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加图标")
            }
        }
    ) { paddingValues ->
        if (icons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "暂无自定义图标",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击右下角 + 号添加",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(icons) { icon ->
                    CustomIconItem(
                        config = icon,
                        onToggleEnabled = { enabled ->
                            val updatedIcons = icons.map {
                                if (it.id == icon.id) it.copy(enabled = enabled) else it
                            }
                            icons = updatedIcons
                            saveIcons(updatedIcons)
                        },
                        onEdit = { onEditIcon(icon) },
                        onDelete = { showDeleteDialog = icon }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { iconToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = {
                Text("确定要删除图标 \"${iconToDelete.id}: ${iconToDelete.title}\" 吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedIcons = icons.filter { it.id != iconToDelete.id }
                        icons = updatedIcons
                        saveIcons(updatedIcons)
                        showDeleteDialog = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun CustomIconItem(
    config: CustomIconConfig,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row: ID, Title, Enable/Disable switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${config.id}: ${config.title}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (config.subtitle.isNotEmpty()) {
                        Text(
                            text = config.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = config.enabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Configuration details
            Text(
                text = "包名: ${config.packageName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!config.activityName.isNullOrEmpty()) {
                Text(
                    text = "活动: ${config.activityName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Launch type badge
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = when (config.launchType) {
                                LaunchType.ACTIVITY -> "Activity"
                                LaunchType.SERVICE -> "Service"
                                LaunchType.BROADCAST -> "Broadcast"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )

                // Root mode badge
                if (config.useRoot) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text("Root模式", style = MaterialTheme.typography.labelSmall)
                        }
                    )
                }

                // New task badge
                if (config.useNewTask) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text("新任务", style = MaterialTheme.typography.labelSmall)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("编辑")
                }
            }
        }
    }
}
