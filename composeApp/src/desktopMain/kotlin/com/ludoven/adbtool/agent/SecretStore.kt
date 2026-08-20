package com.ludoven.adbtool.agent

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.WString
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

interface SecretStore {
    fun write(account: String, secret: String)

    /** Returns null only when the account does not exist; storage failures are reported. */
    fun read(account: String): String?

    /** Missing accounts are treated as already deleted; storage failures are reported. */
    fun delete(account: String)
}

object PlatformSecretStoreFactory {
    fun create(): SecretStore {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("mac") -> MacOsKeychainSecretStore()
            osName.contains("windows") -> WindowsCredentialSecretStore()
            osName.contains("linux") -> LinuxSecretServiceStore()
            else -> UnsupportedSecretStore("Unsupported operating system: $osName")
        }
    }
}

private class UnsupportedSecretStore(
    private val reason: String
) : SecretStore {
    override fun write(account: String, secret: String): Unit = error(reason)
    override fun read(account: String): String? = error(reason)
    override fun delete(account: String): Unit = error(reason)
}

private class MacOsKeychainSecretStore : SecretStore {
    override fun write(account: String, secret: String) {
        val passwordBytes = secret.toByteArray(StandardCharsets.UTF_8)
        val itemReference = PointerByReference()
        try {
            when (val status = findItem(account, itemReference)) {
                ERR_SEC_SUCCESS -> {
                    val item = requireNotNull(itemReference.value)
                    try {
                        checkStatus(
                            operation = "update API key",
                            status = MacOsSecurityFramework.INSTANCE.SecKeychainItemModifyAttributesAndData(
                                itemRef = item,
                                attrList = null,
                                length = passwordBytes.size,
                                data = passwordBytes
                            )
                        )
                    } finally {
                        MacOsCoreFoundation.INSTANCE.CFRelease(item)
                    }
                }
                ERR_SEC_ITEM_NOT_FOUND -> {
                    val serviceBytes = SERVICE_NAME.toByteArray(StandardCharsets.UTF_8)
                    val accountBytes = account.toByteArray(StandardCharsets.UTF_8)
                    checkStatus(
                        operation = "save API key",
                        status = MacOsSecurityFramework.INSTANCE.SecKeychainAddGenericPassword(
                            keychain = null,
                            serviceNameLength = serviceBytes.size,
                            serviceName = serviceBytes,
                            accountNameLength = accountBytes.size,
                            accountName = accountBytes,
                            passwordLength = passwordBytes.size,
                            passwordData = passwordBytes,
                            itemRef = null
                        )
                    )
                }
                else -> checkStatus(operation = "find API key", status = status)
            }
        } finally {
            passwordBytes.fill(0)
        }
    }

    override fun read(account: String): String? {
        val serviceBytes = SERVICE_NAME.toByteArray(StandardCharsets.UTF_8)
        val accountBytes = account.toByteArray(StandardCharsets.UTF_8)
        val passwordLength = IntByReference()
        val passwordData = PointerByReference()
        val status = MacOsSecurityFramework.INSTANCE.SecKeychainFindGenericPassword(
            keychainOrArray = null,
            serviceNameLength = serviceBytes.size,
            serviceName = serviceBytes,
            accountNameLength = accountBytes.size,
            accountName = accountBytes,
            passwordLength = passwordLength,
            passwordData = passwordData,
            itemRef = null
        )
        if (status == ERR_SEC_ITEM_NOT_FOUND) return null
        checkStatus(operation = "read API key", status = status)
        require(passwordLength.value >= 0) { "Unable to read secret from macOS Keychain: invalid data length" }
        val data = passwordData.value
        if (passwordLength.value == 0 && data == null) return ""
        val existingData = requireNotNull(data) { "Unable to read secret from macOS Keychain: missing data" }
        return try {
            existingData.getByteArray(0, passwordLength.value).toString(StandardCharsets.UTF_8)
        } finally {
            MacOsSecurityFramework.INSTANCE.SecKeychainItemFreeContent(null, existingData)
        }
    }

    override fun delete(account: String) {
        val itemReference = PointerByReference()
        when (val status = findItem(account, itemReference)) {
            ERR_SEC_ITEM_NOT_FOUND -> return
            ERR_SEC_SUCCESS -> {
                val item = requireNotNull(itemReference.value)
                try {
                    checkStatus(
                        operation = "delete API key",
                        status = MacOsSecurityFramework.INSTANCE.SecKeychainItemDelete(item)
                    )
                } finally {
                    MacOsCoreFoundation.INSTANCE.CFRelease(item)
                }
            }
            else -> checkStatus(operation = "find API key", status = status)
        }
    }

    private fun findItem(account: String, itemReference: PointerByReference): Int {
        val serviceBytes = SERVICE_NAME.toByteArray(StandardCharsets.UTF_8)
        val accountBytes = account.toByteArray(StandardCharsets.UTF_8)
        return MacOsSecurityFramework.INSTANCE.SecKeychainFindGenericPassword(
            keychainOrArray = null,
            serviceNameLength = serviceBytes.size,
            serviceName = serviceBytes,
            accountNameLength = accountBytes.size,
            accountName = accountBytes,
            passwordLength = null,
            passwordData = null,
            itemRef = itemReference
        )
    }

    private fun checkStatus(operation: String, status: Int) {
        require(status == ERR_SEC_SUCCESS) {
            "Unable to $operation in macOS Keychain (OSStatus $status)"
        }
    }
}

private interface MacOsSecurityFramework : Library {
    fun SecKeychainAddGenericPassword(
        keychain: Pointer?,
        serviceNameLength: Int,
        serviceName: ByteArray,
        accountNameLength: Int,
        accountName: ByteArray,
        passwordLength: Int,
        passwordData: ByteArray,
        itemRef: PointerByReference?
    ): Int

    fun SecKeychainFindGenericPassword(
        keychainOrArray: Pointer?,
        serviceNameLength: Int,
        serviceName: ByteArray,
        accountNameLength: Int,
        accountName: ByteArray,
        passwordLength: IntByReference?,
        passwordData: PointerByReference?,
        itemRef: PointerByReference?
    ): Int

    fun SecKeychainItemModifyAttributesAndData(
        itemRef: Pointer,
        attrList: Pointer?,
        length: Int,
        data: ByteArray
    ): Int

    fun SecKeychainItemFreeContent(attrList: Pointer?, data: Pointer?): Int
    fun SecKeychainItemDelete(itemRef: Pointer): Int

    companion object {
        val INSTANCE: MacOsSecurityFramework = Native.load(
            "/System/Library/Frameworks/Security.framework/Security",
            MacOsSecurityFramework::class.java
        )
    }
}

private interface MacOsCoreFoundation : Library {
    fun CFRelease(value: Pointer)

    companion object {
        val INSTANCE: MacOsCoreFoundation = Native.load(
            "/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation",
            MacOsCoreFoundation::class.java
        )
    }
}

private class LinuxSecretServiceStore : SecretStore {
    override fun write(account: String, secret: String) {
        requireCommand()
        val result = runCommand(
            command = listOf(
                "secret-tool",
                "store",
                "--label=QADB AI API Key",
                "service",
                SERVICE_NAME,
                "account",
                account
            ),
            input = secret,
            timeoutSeconds = COMMAND_TIMEOUT_SECONDS,
            operation = "save secret"
        )
        require(result.exitCode == 0) {
            "Unable to save secret to Linux Secret Service (exit ${result.exitCode})"
        }
    }

    override fun read(account: String): String? {
        requireCommand()
        val result = runCommand(
            command = listOf(
                "secret-tool",
                "lookup",
                "service",
                SERVICE_NAME,
                "account",
                account
            ),
            timeoutSeconds = COMMAND_TIMEOUT_SECONDS,
            operation = "read secret"
        )
        if (result.exitCode == 0) return result.output
        if (result.exitCode == SECRET_TOOL_NOT_FOUND_EXIT_CODE && result.output.isBlank()) return null
        error("Unable to read secret from Linux Secret Service (exit ${result.exitCode})")
    }

    override fun delete(account: String) {
        requireCommand()
        val result = runCommand(
            command = listOf(
                "secret-tool",
                "clear",
                "service",
                SERVICE_NAME,
                "account",
                account
            ),
            timeoutSeconds = COMMAND_TIMEOUT_SECONDS,
            operation = "delete secret"
        )
        if (result.exitCode == 0) return
        if (result.exitCode == SECRET_TOOL_NOT_FOUND_EXIT_CODE && result.output.isBlank()) return
        error("Unable to delete secret from Linux Secret Service (exit ${result.exitCode})")
    }

    private fun requireCommand() {
        val result = runCommand(
            command = listOf("sh", "-c", "command -v secret-tool"),
            timeoutSeconds = 3,
            operation = "locate secret-tool"
        )
        require(result.exitCode == 0) {
            "Linux Secret Service is unavailable. Install secret-tool/libsecret."
        }
    }

    private fun runCommand(
        command: List<String>,
        input: String? = null,
        timeoutSeconds: Long,
        operation: String
    ): SecretToolResult {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        process.outputStream.use { output ->
            input?.let { value ->
                output.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                    writer.write(value)
                    writer.newLine()
                }
            }
        }
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("Linux Secret Service timed out while attempting to $operation")
        }
        return SecretToolResult(
            exitCode = process.exitValue(),
            output = process.inputStream.bufferedReader().use { it.readText() }.trimEnd('\r', '\n')
        )
    }

    private data class SecretToolResult(val exitCode: Int, val output: String)
}

private class WindowsCredentialSecretStore : SecretStore {
    override fun write(account: String, secret: String) {
        val secretBytes = secret.toByteArray(StandardCharsets.UTF_16LE)
        val blob = Memory(secretBytes.size.toLong()).apply { write(0, secretBytes, 0, secretBytes.size) }
        val credential = WinCredential().apply {
            Type = CRED_TYPE_GENERIC
            TargetName = WString(targetName(account))
            CredentialBlobSize = secretBytes.size
            CredentialBlob = blob
            Persist = CRED_PERSIST_LOCAL_MACHINE
            UserName = WString(account)
            write()
        }
        require(CredentialAdvapi32.INSTANCE.CredWriteW(credential, 0)) {
            "Unable to save API key to Windows Credential Manager"
        }
    }

    override fun read(account: String): String? {
        val reference = PointerByReference()
        if (!CredentialAdvapi32.INSTANCE.CredReadW(WString(targetName(account)), CRED_TYPE_GENERIC, 0, reference)) {
            val errorCode = Native.getLastError()
            if (errorCode == WINDOWS_ERROR_NOT_FOUND) return null
            error("Unable to read secret from Windows Credential Manager (Win32 $errorCode)")
        }
        return try {
            val credential = WinCredential(reference.value).apply { read() }
            require(credential.CredentialBlobSize >= 0) {
                "Unable to read secret from Windows Credential Manager: invalid data length"
            }
            if (credential.CredentialBlobSize == 0) {
                ""
            } else {
                val credentialBlob = requireNotNull(credential.CredentialBlob)
                String(
                    credentialBlob.getByteArray(0, credential.CredentialBlobSize),
                    StandardCharsets.UTF_16LE
                )
            }
        } finally {
            CredentialAdvapi32.INSTANCE.CredFree(reference.value)
        }
    }

    override fun delete(account: String) {
        if (CredentialAdvapi32.INSTANCE.CredDeleteW(WString(targetName(account)), CRED_TYPE_GENERIC, 0)) return
        val errorCode = Native.getLastError()
        if (errorCode != WINDOWS_ERROR_NOT_FOUND) {
            error("Unable to delete secret from Windows Credential Manager (Win32 $errorCode)")
        }
    }

    private fun targetName(account: String): String = "$SERVICE_NAME/$account"
}

private interface CredentialAdvapi32 : Library {
    fun CredWriteW(credential: WinCredential, flags: Int): Boolean
    fun CredReadW(targetName: WString, type: Int, flags: Int, credential: PointerByReference): Boolean
    fun CredDeleteW(targetName: WString, type: Int, flags: Int): Boolean
    fun CredFree(pointer: Pointer)

    companion object {
        val INSTANCE: CredentialAdvapi32 = Native.load("Advapi32", CredentialAdvapi32::class.java)
    }
}

@Structure.FieldOrder(
    "Flags",
    "Type",
    "TargetName",
    "Comment",
    "LastWritten",
    "CredentialBlobSize",
    "CredentialBlob",
    "Persist",
    "AttributeCount",
    "Attributes",
    "TargetAlias",
    "UserName"
)
private open class WinCredential() : Structure() {
    @JvmField var Flags: Int = 0
    @JvmField var Type: Int = 0
    @JvmField var TargetName: WString? = null
    @JvmField var Comment: WString? = null
    @JvmField var LastWritten: Long = 0
    @JvmField var CredentialBlobSize: Int = 0
    @JvmField var CredentialBlob: Pointer? = null
    @JvmField var Persist: Int = 0
    @JvmField var AttributeCount: Int = 0
    @JvmField var Attributes: Pointer? = null
    @JvmField var TargetAlias: WString? = null
    @JvmField var UserName: WString? = null

    constructor(pointer: Pointer) : this() {
        useMemory(pointer)
    }
}

private const val SERVICE_NAME = "com.ludoven.adbtool.ai"
private const val COMMAND_TIMEOUT_SECONDS = 15L
private const val ERR_SEC_SUCCESS = 0
private const val ERR_SEC_ITEM_NOT_FOUND = -25300
private const val SECRET_TOOL_NOT_FOUND_EXIT_CODE = 1
private const val WINDOWS_ERROR_NOT_FOUND = 1168
private const val CRED_TYPE_GENERIC = 1
private const val CRED_PERSIST_LOCAL_MACHINE = 2
