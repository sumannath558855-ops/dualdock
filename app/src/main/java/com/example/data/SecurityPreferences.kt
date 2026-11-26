package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.securityDataStore by preferencesDataStore(name = "dualdock_security_prefs")

data class SecuritySettings(
    val isVaultLockEnabled: Boolean = false,
    val hasPin: Boolean = false,
    val pinLength: Int = 4,
    val isBiometricEnabled: Boolean = false,
    val autoLockTimeoutSeconds: Int = 0 // 0 = Immediate, 60 = 1m, 300 = 5m, -1 = Manual only
)

class SecurityPreferences(private val context: Context) {

    private object Keys {
        val VAULT_LOCK_ENABLED = booleanPreferencesKey("vault_lock_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_LENGTH = intPreferencesKey("pin_length")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val AUTO_LOCK_TIMEOUT = intPreferencesKey("auto_lock_timeout_seconds")
    }

    val settingsFlow: Flow<SecuritySettings> = context.securityDataStore.data.map { prefs ->
        val hasPin = !prefs[Keys.PIN_HASH].isNullOrEmpty()
        SecuritySettings(
            isVaultLockEnabled = hasPin && (prefs[Keys.VAULT_LOCK_ENABLED] ?: false),
            hasPin = hasPin,
            pinLength = prefs[Keys.PIN_LENGTH] ?: 4,
            isBiometricEnabled = hasPin && (prefs[Keys.BIOMETRIC_ENABLED] ?: false),
            autoLockTimeoutSeconds = prefs[Keys.AUTO_LOCK_TIMEOUT] ?: 0
        )
    }

    suspend fun getCurrentSettings(): SecuritySettings = settingsFlow.first()

    suspend fun savePin(pin: String, enableVaultLock: Boolean = true) {
        val saltBytes = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val saltHex = saltBytes.toHex()
        val hashHex = hashPin(pin, saltHex)
        context.securityDataStore.edit { prefs ->
            prefs[Keys.PIN_SALT] = saltHex
            prefs[Keys.PIN_HASH] = hashHex
            prefs[Keys.PIN_LENGTH] = pin.length
            prefs[Keys.VAULT_LOCK_ENABLED] = enableVaultLock
        }
    }

    suspend fun verifyPin(candidatePin: String): Boolean {
        val prefs = context.securityDataStore.data.first()
        val savedHash = prefs[Keys.PIN_HASH] ?: return false
        val savedSalt = prefs[Keys.PIN_SALT] ?: return false
        val candidateHash = hashPin(candidatePin, savedSalt)
        return MessageDigest.isEqual(
            savedHash.toByteArray(Charsets.UTF_8),
            candidateHash.toByteArray(Charsets.UTF_8)
        )
    }

    suspend fun setVaultLockEnabled(enabled: Boolean) {
        context.securityDataStore.edit { prefs ->
            val hasPin = !prefs[Keys.PIN_HASH].isNullOrEmpty()
            prefs[Keys.VAULT_LOCK_ENABLED] = enabled && hasPin
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.securityDataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setAutoLockTimeout(seconds: Int) {
        context.securityDataStore.edit { prefs ->
            prefs[Keys.AUTO_LOCK_TIMEOUT] = seconds
        }
    }

    suspend fun removePin() {
        context.securityDataStore.edit { prefs ->
            prefs.remove(Keys.PIN_HASH)
            prefs.remove(Keys.PIN_SALT)
            prefs[Keys.VAULT_LOCK_ENABLED] = false
            prefs[Keys.BIOMETRIC_ENABLED] = false
        }
    }

    private fun hashPin(pin: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$saltHex:$pin:dualdock_vault_v1".toByteArray(Charsets.UTF_8)
        return digest.digest(input).toHex()
    }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { "%02x".format(it) }
}
