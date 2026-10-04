package com.felipechaux.jarvis.core.storage

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/** The device token as a generic-password Keychain item, this device only. */
@OptIn(ExperimentalForeignApi::class)
class KeychainTokenVault(
    private val service: String = "com.felipechaux.jarvis",
    private val account: String = "device_token",
) : TokenVault {

    override suspend fun read(): String? = withBaseQuery(
        kSecReturnData to kCFBooleanTrue,
        kSecMatchLimit to kSecMatchLimitOne,
    ) { query ->
        memScoped {
            val result = alloc<CFTypeRefVar>()
            if (SecItemCopyMatching(query, result.ptr) != errSecSuccess) return@memScoped null
            val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
            NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
        }
    }

    override suspend fun write(token: String?) {
        withBaseQuery { SecItemDelete(it) }
        if (token == null) return
        val data = NSString.create(string = token).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val value = CFBridgingRetain(data)
        try {
            withBaseQuery(
                kSecValueData to value,
                kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            ) { SecItemAdd(it, null) }
        } finally {
            CFRelease(value)
        }
    }

    private inline fun <T> withBaseQuery(vararg extra: Pair<CFTypeRef?, CFTypeRef?>, block: (CFDictionaryRef?) -> T): T {
        val serviceRef = CFBridgingRetain(NSString.create(string = service))
        val accountRef = CFBridgingRetain(NSString.create(string = account))
        val pairs = listOf<Pair<CFTypeRef?, CFTypeRef?>>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceRef,
            kSecAttrAccount to accountRef,
        ) + extra
        val query = CFDictionaryCreateMutable(null, pairs.size.convert(), kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        pairs.forEach { (key, value) -> CFDictionaryAddValue(query, key, value) }
        try {
            return block(query)
        } finally {
            CFRelease(query)
            CFRelease(serviceRef)
            CFRelease(accountRef)
        }
    }
}
