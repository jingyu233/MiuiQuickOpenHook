package com.jingyu233.miuiquickopenhook.ui.activity

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.jingyu233.miuiquickopenhook.data.AppInfo
import com.jingyu233.miuiquickopenhook.ui.theme.MiuiQuickOpenHookTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppPickerActivity : ComponentActivity() {

    companion object {
        private const val TAG = "GXZW-MiuiQuickOpenHook-AppPicker"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_ACTIVITY_NAME = "activity_name"
        const val EXTRA_APP_LABEL = "app_label"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MiuiQuickOpenHookTheme {
                AppPickerScreen(
                    onBackPressed = { finish() },
                    onAppSelected = { appInfo ->
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_PACKAGE_NAME, appInfo.packageName)
                            putExtra(EXTRA_ACTIVITY_NAME, appInfo.launchActivity)
                            putExtra(EXTRA_APP_LABEL, appInfo.label)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppPickerScreen(
        onBackPressed: () -> Unit,
        onAppSelected: (AppInfo) -> Unit
    ) {
        var searchQuery by remember { mutableStateOf("") }
        var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        val scope = rememberCoroutineScope()

        // 加载应用列表
        LaunchedEffect(Unit) {
            scope.launch {
                apps = loadInstalledApps()
                isLoading = false
            }
        }

        // 过滤应用
        val filteredApps = remember(apps, searchQuery) {
            if (searchQuery.isBlank()) {
                apps
            } else {
                apps.filter {
                    it.label.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("选择应用") },
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
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("搜索应用...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    },
                    singleLine = true
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(filteredApps) { app ->
                            AppListItem(
                                appInfo = app,
                                onClick = { onAppSelected(app) }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AppListItem(
        appInfo: AppInfo,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 应用图标
                Image(
                    bitmap = appInfo.icon.toBitmap(72, 72).asImageBitmap(),
                    contentDescription = appInfo.label,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 应用信息
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appInfo.label,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = appInfo.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (appInfo.launchActivity != null) {
                        Text(
                            text = appInfo.launchActivity,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        try {
            val pm = packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { app ->
                    // 只显示用户应用或有启动器的系统应用
                    (app.flags and ApplicationInfo.FLAG_SYSTEM == 0) ||
                    pm.getLaunchIntentForPackage(app.packageName) != null
                }
                .mapNotNull { app ->
                    try {
                        val label = pm.getApplicationLabel(app).toString()
                        val icon = pm.getApplicationIcon(app)

                        // 获取启动Activity
                        val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                        val launchActivity = launchIntent?.component?.className

                        AppInfo(
                            packageName = app.packageName,
                            label = label,
                            icon = icon,
                            launchActivity = launchActivity
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load app info for ${app.packageName}: ${e.message}")
                        null
                    }
                }
                .sortedBy { it.label.lowercase() }

            Log.i(TAG, "Loaded ${apps.size} apps")
            apps
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load apps: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
}
