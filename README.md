# MiuiQuickOpenHook

[![GitHub license](https://img.shields.io/github/license/jingyu233/MiuiQuickOpenHook)](https://github.com/jingyu233/MiuiQuickOpenHook/blob/main/LICENSE)
[![GitHub release](https://img.shields.io/github/v/release/jingyu233/MiuiQuickOpenHook)](https://github.com/jingyu233/MiuiQuickOpenHook/releases)
[![GitHub stars](https://img.shields.io/github/stars/jingyu233/MiuiQuickOpenHook)](https://github.com/jingyu233/MiuiQuickOpenHook/stargazers)
[![LSPosed Repo](https://img.shields.io/badge/LSPosed-Module-blue)](https://github.com/Xposed-Modules-Repo/com.jingyu233.miuiquickopenhook)

一个强大的 Xposed 模块，用于自定义 MIUI/HyperOS 系统的快速打开（Quick Open）功能。在光学指纹机型上，通过光学指纹解锁后持续按住，可以在解锁界面呼出快捷打开面板。本模块旨在 Hook 该功能，为用户提供除系统默认外的更多自定义可能性。

**如果这个项目对你有帮助，请给它一个 ⭐ Star！**

## 下载

- **LSPosed 模块仓库**: [Xposed-Modules-Repo/com.jingyu233.miuiquickopenhook](https://github.com/Xposed-Modules-Repo/com.jingyu233.miuiquickopenhook)
- **GitHub Releases**: [下载最新版本](https://github.com/jingyu233/MiuiQuickOpenHook/releases)

---

## 功能特性

✨ **扩展快捷项** - 添加更多应用到光学指纹快捷面板

🎨 **自定义图标** - 为快捷项设置个性化图标

📱 **灵活配置** - 支持多种启动方式和布局配置

🛠️ **实时调整** - 无需重启即可应用配置更改

🎯 **精准 Hook** - 针对光学指纹解锁区域深度定制

## 安装要求

- Android 设备（MIUI / HyperOS 系统）
- Root 权限
- [LSPosed](https://github.com/LSPosed/LSPosed) 框架
- 其他环境未测试

## 测试环境

- 小米14
- HyperOS 3.0.4.0.WNCCNXM
- LSPosed 1.9.2-it

## 使用方法

### 安装模块
1. 下载并安装 APK 文件
2. 在 LSPosed 管理器中启用模块
3. 重启设备或重启 SystemUI 进程使模块生效

### 生效方式
- 保存配置后自动应用更改
- 无需重启系统UI

## 调试信息

在 debug 版本中，大部分调试日志都带有 `GXZW-MiuiQuickOpenHook` 标签，便于追踪和排查问题。

## 最佳实践配置示例

以下是一些常用的快捷方式配置示例：

### 哈啰快速扫码

直接启动 Activity 打开哈啰出行的扫码界面：

| 配置项 | 值 |
|--------|---|
| 包名 | `com.jingyao.easybike` |
| Activity | `com.hellobike.platform.scan.v2.NewPlatformScanActivity` |
| 启动方式 | Activity |

### 支付宝出行码

通过 Intent Data URI 快速打开支付宝出行码：

| 配置项 | 值 |
|--------|---|
| 包名 | `com.eg.android.AlipayGphone` |
| Activity | `com.eg.android.AlipayGphone.AlipayLogin` |
| Intent Data | `alipayqr://platformapi/startapp?saId=200011235` |
| 启动方式 | Activity + Data URI |

## 构建项目

```bash
./gradlew assembleRelease
```

构建完成的 APK 文件将位于 `app/build/outputs/apk/` 目录。

## 许可证

本项目采用 MIT 许可证.

---

# MiuiQuickOpenHook

[![GitHub license](https://img.shields.io/github/license/jingyu233/MiuiQuickOpenHook)](https://github.com/jingyu233/MiuiQuickOpenHook/blob/main/LICENSE)
[![GitHub release](https://img.shields.io/github/v/release/jingyu233/MiuiQuickOpenHook)](https://github.com/jingyu233/MiuiQuickOpenHook/releases)
[![GitHub stars](https://img.shields.io/github/stars/jingyu233/MiuiQuickOpenHook)](https://github.com/jingyu233/MiuiQuickOpenHook/stargazers)
[![LSPosed Repo](https://img.shields.io/badge/LSPosed-Module-blue)](https://github.com/Xposed-Modules-Repo/com.jingyu233.miuiquickopenhook)

A powerful Xposed module for customizing MIUI/HyperOS Quick Open functionality. On optical fingerprint devices, after unlocking via optical fingerprint, keeping your finger pressed can invoke the quick open panel on the lock screen. This module hooks into that feature to provide users with more customization possibilities beyond the system defaults.

**If this project is helpful to you, please give it a ⭐ Star!**

## Download

- **LSPosed Module Repo**: [Xposed-Modules-Repo/com.jingyu233.miuiquickopenhook](https://github.com/Xposed-Modules-Repo/com.jingyu233.miuiquickopenhook)
- **GitHub Releases**: [Download Latest](https://github.com/jingyu233/MiuiQuickOpenHook/releases)

## Features

✨ **Extended Shortcuts** - Add more apps to the optical fingerprint quick panel

🎨 **Custom Icons** - Set personalized icons for shortcuts

📱 **Flexible Configuration** - Support multiple launch types and layouts

🛠️ **Real-time Updates** - Apply changes without reboot

🎯 **Precise Hooking** - Deep customization for optical fingerprint unlock area

## Requirements

- Android device (MIUI / HyperOS system)
- Root access
- [LSPosed](https://github.com/LSPosed/LSPosed) framework
- Other environments not tested

## Tested Environment

- Xiaomi 14
- HyperOS 3.0.4.0.WNCCNXM
- LSPosed 1.9.2-it

## Installation

### Install Module
1. Download and install the APK file
2. Enable the module in LSPosed Manager
3. Reboot device or restart SystemUI process

### Apply Changes
- Auto-apply changes after saving
- No SystemUI restart required

## Debug Information

Most debug logs include the tag `GXZW-MiuiQuickOpenHook` for easy tracking and troubleshooting.

## Best Practices Configuration Examples

Here are some commonly used shortcut configuration examples:

### Hello Bike Quick Scan

Launch Activity directly to open Hello Bike's scan interface:

| Config | Value |
|--------|-------|
| Package | `com.jingyao.easybike` |
| Activity | `com.hellobike.platform.scan.v2.NewPlatformScanActivity` |
| Launch Type | Activity |

### Alipay Transit Code

Open Alipay transit code via Intent Data URI:

| Config | Value |
|--------|-------|
| Package | `com.eg.android.AlipayGphone` |
| Activity | `com.eg.android.AlipayGphone.AlipayLogin` |
| Intent Data | `alipayqr://platformapi/startapp?saId=200011235` |
| Launch Type | Activity + Data URI |

## Build

```bash
./gradlew assembleRelease
```

The built APK will be located in `app/build/outputs/apk/`.

## License

This project is licensed under the MIT License.
