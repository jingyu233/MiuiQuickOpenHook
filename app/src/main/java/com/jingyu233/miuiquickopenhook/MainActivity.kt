package com.jingyu233.miuiquickopenhook

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.jingyu233.miuiquickopenhook.config.AppConfig
import com.jingyu233.miuiquickopenhook.manager.PreferencesManager
import com.jingyu233.miuiquickopenhook.ui.activity.ConfigActivity
import com.jingyu233.miuiquickopenhook.ui.activity.CustomIconManagementActivity
import com.jingyu233.miuiquickopenhook.ui.theme.MiuiQuickOpenHookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiuiQuickOpenHookTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onOpenConfig = {
                            startActivity(Intent(this, ConfigActivity::class.java))
                        },
                        onOpenCustomIcons = {
                            startActivity(Intent(this, CustomIconManagementActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onOpenConfig: () -> Unit,
    onOpenCustomIcons: () -> Unit = {}
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var showHideIconDialog by remember { mutableStateOf(false) }

    // Check if module is enabled
    val isModuleActive = remember {
        try {
            val prefs = PreferencesManager.getModuleStatusPreferences(context)
            prefs.getBoolean(AppConfig.KEY_XPOSED_ACTIVE, false)
        } catch (e: Exception) {
            false
        }
    }

    // Check if desktop icon is hidden (SplashActivity corresponds to desktop icon)
    var isIconHidden by remember {
        mutableStateOf(
            context.packageManager.getComponentEnabledSetting(
                ComponentName(context, SplashActivity::class.java)
            ) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        )
    }

    // Configuration file path
    val prefsPath = "${context.dataDir}/shared_prefs/${AppConfig.PREFS_CONFIG_NAME}.xml"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MIUI快速打开Hook模块",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 模块状态指示器
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isModuleActive) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (isModuleActive) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Warning
                    },
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isModuleActive) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isModuleActive) "模块已激活" else "模块未激活",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isModuleActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    Text(
                        text = if (isModuleActive) {
                            "模块正常工作中"
                        } else {
                            "请在LSPosed中启用并重启SystemUI"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isModuleActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onOpenConfig,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("配置管理")
            }

            Button(
                onClick = onOpenCustomIcons,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("图标管理")
            }

            OutlinedButton(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("清空配置")
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "隐藏桌面图标",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "LSPosed仍可打开",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Switch(
                    checked = isIconHidden,
                    onCheckedChange = { hideIcon ->
                        if (hideIcon) {
                            showHideIconDialog = true
                        } else {
                            context.packageManager.setComponentEnabledSetting(
                                ComponentName(context, SplashActivity::class.java),
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                PackageManager.DONT_KILL_APP
                            )
                            isIconHidden = false
                            Toast.makeText(context, "桌面图标已显示", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        val annotatedText = buildAnnotatedString {
            append("在MIUI/HyperOS中，使用光学指纹解锁屏幕后，依旧长按并向上滑动，可以快速选择各种快捷方式，该模块对此功能进行增强。\n如果有用，")

            pushStringAnnotation("github", "https://github.com/jingyu233/MiuiQuickOpenHook")
            withStyle(
                style = androidx.compose.ui.text.SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append("在GitHub上点个Star")
            }
            pop()

            append("。这会对我")

            withStyle(
                style = androidx.compose.ui.text.SpanStyle(
                    textDecoration = TextDecoration.LineThrough
                )
            ) {
                append("找工作")
            }

            append("有很大的帮助。")
        }

        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable {
                    annotatedText.getStringAnnotations("github", 0, annotatedText.length)
                        .firstOrNull()?.let { annotation ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                            context.startActivity(intent)
                        }
                }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "配置文件路径:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = prefsPath,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    // 清空配置确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清空配置") },
            text = { Text("确定要清空所有配置吗？此操作不可撤销！") },
            confirmButton = {
                TextButton(
                    onClick = {
                        @Suppress("DEPRECATION")
                        val prefs = try {
                            context.getSharedPreferences("miui_config", Context.MODE_WORLD_READABLE)
                        } catch (e: SecurityException) {
                            context.getSharedPreferences("miui_config", Context.MODE_PRIVATE)
                        }
                        prefs.edit().clear().apply()
                        Toast.makeText(context, "配置已清空", Toast.LENGTH_SHORT).show()
                        showClearDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 隐藏图标确认对话框
    if (showHideIconDialog) {
        AlertDialog(
            onDismissRequest = { showHideIconDialog = false },
            title = { Text("隐藏桌面图标") },
            text = {
                Column {
                    Text("隐藏桌面图标后，可以通过以下方式打开：")
                    Text("\n1. 在LSPosed模块列表中点击【设置】按钮")
                    Text("2. 使用应用管理器打开")
                    Text("隐藏后LSPosed仍可直接打开设置页面。")
                    Text("\n需要关闭LSPosed设置中的【强制显示桌面图标】选项。")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 隐藏图标 - 禁用SplashActivity（带LAUNCHER的入口）
                        context.packageManager.setComponentEnabledSetting(
                            ComponentName(context, SplashActivity::class.java),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                        isIconHidden = true
                        showHideIconDialog = false
                        Toast.makeText(context, "桌面图标已隐藏", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("确定隐藏")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHideIconDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}