package com.jingyu233.miuiquickopenhook.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.jingyu233.miuiquickopenhook.config.AppConfig
import de.robv.android.xposed.XSharedPreferences
import java.io.File

/**
 * Manages SharedPreferences instances across the application
 *
 * This singleton provides centralized management of SharedPreferences
 * with proper handling of MODE_WORLD_READABLE compatibility issues
 * across different Android versions.
 */
object PreferencesManager {

    private const val TAG = "PreferencesManager"

    /**
     * Get configuration SharedPreferences instance
     *
     * @param context Application context
     * @return SharedPreferences instance for module configuration
     */
    fun getConfigPreferences(context: Context): SharedPreferences {
        return getSharedPreferences(context, AppConfig.PREFS_CONFIG_NAME)
    }

    /**
     * Get module status SharedPreferences instance
     *
     * @param context Application context
     * @return SharedPreferences instance for module status tracking
     */
    fun getModuleStatusPreferences(context: Context): SharedPreferences {
        return getSharedPreferences(context, AppConfig.PREFS_MODULE_STATUS_NAME)
    }

    /**
     * Get XSharedPreferences for cross-process communication (LSPosed)
     *
     * @return XSharedPreferences instance for reading config from SystemUI process
     */
    fun getXSharedPreferences(): XSharedPreferences {
        return XSharedPreferences(AppConfig.MODULE_PACKAGE, AppConfig.PREFS_CONFIG_NAME)
    }

    /**
     * Get XSharedPreferences with specific file name
     *
     * @param packageName Package name that owns the preferences
     * @param fileName Preference file name
     * @return XSharedPreferences instance
     */
    fun getXSharedPreferences(packageName: String, fileName: String): XSharedPreferences {
        return XSharedPreferences(packageName, fileName)
    }

    /**
     * Generic method to get SharedPreferences with fallback
     *
     * @param context Application context
     * @param name Preference file name
     * @return SharedPreferences instance with automatic fallback
     */
    private fun getSharedPreferences(context: Context, name: String): SharedPreferences {
        return try {
            @Suppress("DEPRECATION")
            context.getSharedPreferences(name, Context.MODE_WORLD_READABLE)
        } catch (e: SecurityException) {
            Log.w(TAG, "MODE_WORLD_READABLE not available, using MODE_PRIVATE for $name")
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing preferences: $name", e)
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
        }
    }

    /**
     * Check if XSharedPreferences file is readable
     *
     * @param prefs XSharedPreferences instance to check
     * @return true if file is readable, false otherwise
     */
    fun isXSharedPreferencesReadable(prefs: XSharedPreferences): Boolean {
        return try {
            prefs.file.canRead()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking XSharedPreferences readability", e)
            false
        }
    }

    /**
     * Get XSharedPreferences file path for logging
     *
     * @param prefs XSharedPreferences instance
     * @return Absolute file path as string
     */
    fun getXSharedPreferencesFilePath(prefs: XSharedPreferences): String {
        return try {
            prefs.file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error getting XSharedPreferences file path", e)
            "unknown"
        }
    }

    /**
     * Check if XSharedPreferences file exists
     *
     * @param prefs XSharedPreferences instance
     * @return true if file exists, false otherwise
     */
    fun doesXSharedPreferencesExist(prefs: XSharedPreferences): Boolean {
        return try {
            prefs.file.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking XSharedPreferences existence", e)
            false
        }
    }

    /**
     * Get preferences file size in bytes
     *
     * @param prefs XSharedPreferences instance
     * @return File size in bytes, or -1 if unable to determine
     */
    fun getXSharedPreferencesFileSize(prefs: XSharedPreferences): Long {
        return try {
            val file = prefs.file
            if (file.exists()) file.length() else -1
        } catch (e: Exception) {
            Log.e(TAG, "Error getting XSharedPreferences file size", e)
            -1
        }
    }

    /**
     * Clear all preferences
     *
     * @param context Application context
     * @param name Preference file name to clear
     * @return true if successful, false otherwise
     */
    fun clearPreferences(context: Context, name: String): Boolean {
        return try {
            val prefs = getSharedPreferences(context, name)
            prefs.edit().clear().apply()
            Log.i(TAG, "Successfully cleared preferences: $name")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing preferences: $name", e)
            false
        }
    }

    /**
     * Remove specific key from preferences
     *
     * @param context Application context
     * @param name Preference file name
     * @param key Key to remove
     * @return true if successful, false otherwise
     */
    fun removeKey(context: Context, name: String, key: String): Boolean {
        return try {
            val prefs = getSharedPreferences(context, name)
            prefs.edit().remove(key).apply()
            Log.d(TAG, "Removed key '$key' from preferences: $name")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing key '$key' from preferences: $name", e)
            false
        }
    }

    /**
     * Check if a preference key exists
     *
     * @param prefs SharedPreferences instance
     * @param key Key to check
     * @return true if key exists, false otherwise
     */
    fun hasKey(prefs: SharedPreferences, key: String): Boolean {
        return prefs.contains(key)
    }

    /**
     * Get all keys from preferences
     *
     * @param prefs SharedPreferences instance
     * @return Set of all keys
     */
    fun getAllKeys(prefs: SharedPreferences): Set<String> {
        return try {
            prefs.all.keys
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all keys from preferences", e)
            emptySet()
        }
    }

    /**
     * Get preferences info for debugging
     *
     * @param prefs SharedPreferences instance
     * @param name Preference file name
     * @return Debug information string
     */
    fun getPreferencesDebugInfo(prefs: SharedPreferences, name: String): String {
        return try {
            val allKeys = getAllKeys(prefs)
            val keyCount = allKeys.size
            "Preferences: $name\nKeys: $keyCount\nHas Config: ${hasKey(prefs, AppConfig.KEY_ROW_CONFIGS)}\nHas Custom Icons: ${hasKey(prefs, AppConfig.KEY_CUSTOM_ICONS)}"
        } catch (e: Exception) {
            "Error getting debug info: ${e.message}"
        }
    }
}
