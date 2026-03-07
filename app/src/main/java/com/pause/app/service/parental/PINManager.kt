package com.pause.app.service.parental

import at.favre.lib.crypto.bcrypt.BCrypt
import com.pause.app.data.preferences.PreferencesManager
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

sealed class PINResult {
    object Correct : PINResult()
    data class WrongPin(val attemptsRemaining: Int) : PINResult()
    object LockedOut : PINResult()
}

@Singleton
class PINManager @Inject constructor(
    private val preferencesManager: PreferencesManager
) {

    suspend fun setupPIN(rawPin: String): Boolean {
        val hash = BCrypt.withDefaults().hashToString(COST_FACTOR, rawPin.toCharArray())
        preferencesManager.setPinBcryptHash(hash)
        preferencesManager.setPinAttemptCount(0)
        preferencesManager.setPinLockoutUntil(0L)
        return true
    }

    suspend fun verifyPIN(rawPin: String): PINResult {
        if (isLockedOut()) return PINResult.LockedOut
        val hash = preferencesManager.getPinBcryptHash() ?: return PINResult.WrongPin(MAX_ATTEMPTS - 1)
        val verified = BCrypt.verifyer().verify(rawPin.toCharArray(), hash).verified
        return if (verified) {
            preferencesManager.setPinAttemptCount(0)
            preferencesManager.setPinLockoutUntil(0L)
            PINResult.Correct
        } else {
            val count = preferencesManager.getPinAttemptCount() + 1
            preferencesManager.setPinAttemptCount(count)
            if (count >= MAX_ATTEMPTS) {
                val lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
                preferencesManager.setPinLockoutUntil(lockoutUntil)
                PINResult.LockedOut
            } else {
                PINResult.WrongPin(MAX_ATTEMPTS - count - 1)
            }
        }
    }

    suspend fun isLockedOut(): Boolean {
        val until = preferencesManager.getPinLockoutUntil()
        if (until <= 0) return false
        return System.currentTimeMillis() < until
    }

    suspend fun getLockoutRemainingMs(): Long {
        val until = preferencesManager.getPinLockoutUntil()
        if (until <= 0) return 0
        return (until - System.currentTimeMillis()).coerceAtLeast(0)
    }

    suspend fun setupRecoveryPhrase(phrase: String) {
        val hash = sha256(phrase.trim())
        preferencesManager.setRecoveryPhraseHash(hash)
    }

    suspend fun verifyRecoveryPhrase(phrase: String): Boolean {
        val stored = preferencesManager.getRecoveryPhraseHash() ?: return false
        return sha256(phrase.trim()) == stored
    }

    suspend fun resetPINWithPhrase(phrase: String, newPin: String): Boolean {
        if (!verifyRecoveryPhrase(phrase)) return false
        setupPIN(newPin)
        preferencesManager.setPinAttemptCount(0)
        preferencesManager.setPinLockoutUntil(0L)
        return true
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val COST_FACTOR = 12
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 600_000L
    }
}
