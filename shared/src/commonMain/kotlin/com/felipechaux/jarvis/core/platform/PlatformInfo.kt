package com.felipechaux.jarvis.core.platform

/**
 * How this phone introduces itself when pairing ("Pixel 9", "android") and
 * the language sent to Whisper ("es"), from the device locale.
 */
data class PlatformInfo(val deviceName: String, val platform: String, val language: String)
