package com.movtery.zalithlauncher.game.account

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts / decrypts a blob; the blob layout (IV first) is private to the implementation */
interface SecretCipher {
    fun encrypt(plain: ByteArray): ByteArray
    fun decrypt(blob: ByteArray): ByteArray
}

/**
 * AES-256-GCM with a key that lives in the Android Keystore (non-exportable, bound to this app, never in a backup:
 * allowBackup is off). The key is created on first use.
 */
class KeystoreSecretCipher(private val alias: String = "all_account_secrets_v1") : SecretCipher {
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    override fun encrypt(plain: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        return c.iv + c.doFinal(plain)
    }

    override fun decrypt(blob: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, blob.copyOfRange(0, 12)))
        return c.doFinal(blob, 12, blob.size - 12)
    }
}

/**
 * Encryption at rest for the secrets stored in the accounts table (Microsoft access / refresh token, password of
 * third-party auth servers). In memory an [Account] always holds the plain values; only the database row is sealed.
 *
 * Format: `enc1:` + Base64(IV || ciphertext+tag). Values that are not secrets ("", "0", "None") stay as they are, and values
 * without the prefix are treated as legacy plaintext: they are read as they are and sealed on the next load (migration).
 * A sealed value that cannot be opened (key lost, tampered) becomes "0": the account stays in the list and asks for a new
 * login instead of being deleted, and nothing secret is ever guessed.
 */
object AccountSecrets {
    const val PREFIX = "enc1:"
    private val placeholders = setOf("", "0", "None", "NULL")

    @Volatile
    var cipher: SecretCipher = KeystoreSecretCipher()

    fun isSealed(value: String) = value.startsWith(PREFIX)

    /** true for a real secret that is not encrypted yet */
    fun isPlainSecret(value: String) = value !in placeholders && !isSealed(value)

    fun seal(value: String): String {
        if (!isPlainSecret(value)) return value
        return PREFIX + Base64.getEncoder().encodeToString(cipher.encrypt(value.toByteArray(Charsets.UTF_8)))
    }

    fun open(value: String): String {
        if (!isSealed(value)) return value
        return runCatching {
            String(cipher.decrypt(Base64.getDecoder().decode(value.removePrefix(PREFIX))), Charsets.UTF_8)
        }.getOrDefault("0")
    }
}

fun Account.hasPlainSecrets(): Boolean =
    AccountSecrets.isPlainSecret(accessToken) || AccountSecrets.isPlainSecret(refreshToken) ||
        otherPassword?.let { AccountSecrets.isPlainSecret(it) } == true

/** copy for the database row */
fun Account.sealed(): Account = copy(
    accessToken = AccountSecrets.seal(accessToken),
    refreshToken = AccountSecrets.seal(refreshToken),
    otherPassword = otherPassword?.let(AccountSecrets::seal)
)

/** copy for memory (plain values) */
fun Account.opened(): Account = copy(
    accessToken = AccountSecrets.open(accessToken),
    refreshToken = AccountSecrets.open(refreshToken),
    otherPassword = otherPassword?.let(AccountSecrets::open)
)
