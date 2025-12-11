package com.jingyu233.miuiquickopenhook.util

import android.util.Log
import de.robv.android.xposed.XposedBridge
import com.jingyu233.miuiquickopenhook.config.AppConfig

/**
 * Centralized logging utility for Xposed module
 *
 * This object provides unified logging and error handling across the entire module,
 * eliminating code duplication and ensuring consistent log formatting.
 */
object XposedLogger {

    // ============================================================
    // INFO LEVEL LOGGING
    // ============================================================

    /**
     * Log an informational message with a tag
     */
    fun info(tag: String, message: String) {
        XposedBridge.log("[$tag-Info] $message")
    }

    /**
     * Log an informational message using the default tag
     */
    fun info(message: String) {
        info(AppConfig.LOG_TAG, message)
    }

    /**
     * Log an informational message with formatted arguments
     */
    fun info(tag: String, message: String, vararg args: Any) {
        info(tag, message.format(*args))
    }

    /**
     * Log an informational message with formatted arguments (default tag)
     */
    fun info(message: String, vararg args: Any) {
        info(AppConfig.LOG_TAG, message.format(*args))
    }


    // ============================================================
    // DEBUG LEVEL LOGGING
    // ============================================================

    /**
     * Log a debug message with a tag
     */
    fun debug(tag: String, message: String) {
        XposedBridge.log("[$tag-Debug] $message")
    }

    /**
     * Log a debug message using the default tag
     */
    fun debug(message: String) {
        debug(AppConfig.LOG_TAG, message)
    }

    /**
     * Log a debug message with formatted arguments
     */
    fun debug(tag: String, message: String, vararg args: Any) {
        debug(tag, message.format(*args))
    }

    /**
     * Log a debug message with formatted arguments (default tag)
     */
    fun debug(message: String, vararg args: Any) {
        debug(AppConfig.LOG_TAG, message.format(*args))
    }


    // ============================================================
    // WARNING LEVEL LOGGING
    // ============================================================

    /**
     * Log a warning message with a tag
     */
    fun warning(tag: String, message: String) {
        XposedBridge.log("[$tag-Warning] $message")
    }

    /**
     * Log a warning message using the default tag
     */
    fun warning(message: String) {
        warning(AppConfig.LOG_TAG, message)
    }

    /**
     * Log a warning message with formatted arguments
     */
    fun warning(tag: String, message: String, vararg args: Any) {
        warning(tag, message.format(*args))
    }

    /**
     * Log a warning message with formatted arguments (default tag)
     */
    fun warning(message: String, vararg args: Any) {
        warning(AppConfig.LOG_TAG, message.format(*args))
    }


    // ============================================================
    // ERROR LEVEL LOGGING
    // ============================================================

    /**
     * Log an error message with a tag
     */
    fun error(tag: String, message: String) {
        XposedBridge.log("[$tag-Error] $message")
    }

    /**
     * Log an error message using the default tag
     */
    fun error(message: String) {
        error(AppConfig.LOG_TAG, message)
    }

    /**
     * Log an error message with formatted arguments
     */
    fun error(tag: String, message: String, vararg args: Any) {
        error(tag, message.format(*args))
    }

    /**
     * Log an error message with formatted arguments (default tag)
     */
    fun error(message: String, vararg args: Any) {
        error(AppConfig.LOG_TAG, message.format(*args))
    }


    // ============================================================
    // EXCEPTION HANDLING
    // ============================================================

    /**
     * Log an error with an exception and print stack trace
     */
    fun error(tag: String, message: String, throwable: Throwable?) {
        error(tag, message)
        throwable?.printStackTrace()
        // Also log to Android log for additional visibility
        Log.e(tag, message, throwable)
    }

    /**
     * Log an error with an exception using the default tag
     */
    fun error(message: String, throwable: Throwable?) {
        error(AppConfig.LOG_TAG, message, throwable)
    }

    /**
     * Log an error with formatted message and exception
     */
    fun error(tag: String, message: String, throwable: Throwable?, vararg args: Any) {
        error(tag, message.format(*args), throwable)
    }

    /**
     * Log an error with formatted message and exception (default tag)
     */
    fun error(message: String, throwable: Throwable?, vararg args: Any) {
        error(AppConfig.LOG_TAG, message.format(*args), throwable)
    }

    /**
     * Log an exception with default error message
     */
    fun exception(tag: String, throwable: Throwable) {
        error(tag, "Exception occurred: ${throwable.message}", throwable as Throwable?)
    }

    /**
     * Log an exception using default tag
     */
    fun exception(throwable: Throwable) {
        exception(AppConfig.LOG_TAG, throwable)
    }


    // ============================================================
    // SPECIALIZED LOGGING METHODS
    // ============================================================

    /**
     * Log configuration-related messages
     */
    fun config(message: String, vararg args: Any) {
        info(AppConfig.LOG_TAG_CONFIG, message, *args)
    }

    /**
     * Log configuration errors
     */
    fun configError(message: String, throwable: Throwable? = null) {
        error(AppConfig.LOG_TAG_CONFIG, message, throwable)
    }

    /**
     * Log custom icon-related messages
     */
    fun custom(message: String, vararg args: Any) {
        info(AppConfig.LOG_TAG_CUSTOM, message, *args)
    }

    /**
     * Log custom icon errors
     */
    fun customError(message: String, throwable: Throwable? = null) {
        error(AppConfig.LOG_TAG_CUSTOM, message, throwable)
    }

    /**
     * Log hook success messages
     */
    fun hookSuccess(methodName: String) {
        info(AppConfig.LOG_TAG, "✓ Successfully hooked $methodName")
    }

    /**
     * Log hook failure messages
     */
    fun hookFailure(methodName: String, throwable: Throwable) {
        error(AppConfig.LOG_TAG, "Failed to hook $methodName", throwable as Throwable?)
    }

    /**
     * Log hook start messages
     */
    fun hookStart(className: String) {
        info(AppConfig.LOG_TAG, "Hooking $className")
    }

    /**
     * Log hook completed successfully
     */
    fun hookCompleted() {
        info(AppConfig.LOG_TAG, "Hook completed successfully")
    }

    /**
     * Log hook failed
     */
    fun hookFailed(message: String, throwable: Throwable? = null) {
        error(AppConfig.LOG_TAG, "Hook failed: $message", throwable)
    }


    // ============================================================
    // UTILITY METHODS
    // ============================================================

    /**
     * Execute a block with error handling and logging
     */
    fun <T> executeWithLogging(tag: String, operation: String, block: () -> T): T? {
        return try {
            block()
        } catch (e: Throwable) {
            error(tag, "Error during $operation: ${e.message}", e as Throwable?)
            null
        }
    }

    /**
     * Execute a block with error handling using default tag
     */
    fun <T> executeWithLogging(operation: String, block: () -> T): T? {
        return executeWithLogging(AppConfig.LOG_TAG, operation, block)
    }

    /**
     * Execute a block with error handling and logging, returning a default value on exception
     */
    fun <T> executeWithLogging(tag: String, operation: String, defaultValue: T, block: () -> T): T {
        return try {
            block()
        } catch (e: Throwable) {
            error(tag, "Error during $operation: ${e.message}", e as Throwable?)
            defaultValue
        }
    }

    /**
     * Execute a block with error handling using default tag
     */
    fun <T> executeWithLogging(operation: String, defaultValue: T, block: () -> T): T {
        return executeWithLogging(AppConfig.LOG_TAG, operation, defaultValue, block)
    }

    /**
     * Log a separator line for better log readability
     */
    fun separator(char: Char = '=', length: Int = 50) {
        val separator = char.toString().repeat(length)
        XposedBridge.log("[$AppConfig.LOG_TAG] $separator")
    }

    /**
     * Log a section header
     */
    fun section(title: String) {
        separator('=', 50)
        info(title)
        separator('-', 50)
    }
}
