package com.movtery.zalithlauncher.game.account

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Software AES-GCM stand-in for the Android Keystore cipher (same blob layout: IV || ciphertext) */
private class TestCipher(keyBytes: ByteArray = ByteArray(32) { it.toByte() }) : SecretCipher {
    private val key = SecretKeySpec(keyBytes, "AES")
    override fun encrypt(plain: ByteArray): ByteArray {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv)) }
        return iv + c.doFinal(plain)
    }
    override fun decrypt(blob: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, blob.copyOfRange(0, 12))) }
        return c.doFinal(blob, 12, blob.size - 12)
    }
}

class AccountSecretsTest {
    private val original = AccountSecrets.cipher

    @Before fun setUp() { AccountSecrets.cipher = TestCipher() }
    @After fun tearDown() { AccountSecrets.cipher = original }

    @Test
    fun sealedValueIsNotThePlainTokenAndOpensBack() {
        val token = "eyJ.fake-access-token.value"
        val sealed = AccountSecrets.seal(token)
        assertTrue(sealed.startsWith("enc1:"))
        assertFalse(sealed.contains(token))
        assertEquals(token, AccountSecrets.open(sealed))
        //random IV: sealing twice gives different text, both open to the same value
        assertNotEquals(sealed, AccountSecrets.seal(token))
    }

    @Test
    fun placeholdersAndAlreadySealedValuesAreLeftAlone() {
        listOf("", "0", "None").forEach { assertEquals(it, AccountSecrets.seal(it)); assertEquals(it, AccountSecrets.open(it)) }
        val s = AccountSecrets.seal("secret")
        assertEquals(s, AccountSecrets.seal(s))
    }

    @Test
    fun legacyPlaintextIsReadAsItIs() {
        assertEquals("old-plain-refresh-token", AccountSecrets.open("old-plain-refresh-token"))
        assertTrue(AccountSecrets.isPlainSecret("old-plain-refresh-token"))
    }

    @Test
    fun aTamperedOrForeignKeyValueNeverLeaksAndNeverCrashes() {
        val sealed = AccountSecrets.seal("secret")
        val flipped = sealed.dropLast(3) + (if (sealed.takeLast(3) == "AAA") "BBB" else "AAA")
        assertEquals("0", AccountSecrets.open(flipped))
        AccountSecrets.cipher = TestCipher(ByteArray(32) { (it + 1).toByte() }) //another key = the Keystore key was lost
        assertEquals("0", AccountSecrets.open(sealed))
    }

    @Test
    fun accountRowIsSealedAndMemoryCopyIsPlain() {
        val acc = Account(accessToken = "AT-123", refreshToken = "RT-456", username = "QA", otherPassword = "pw-789")
        assertTrue(acc.hasPlainSecrets())
        val row = acc.sealed()
        assertFalse(row.hasPlainSecrets())
        assertFalse(listOf(row.accessToken, row.refreshToken, row.otherPassword.orEmpty()).any { it.contains("AT-123") || it.contains("RT-456") || it.contains("pw-789") })
        assertEquals(acc, row.opened())
        assertEquals("QA", row.username)
        assertEquals(acc.uniqueUUID, row.uniqueUUID)
    }

    @Test
    fun offlineAccountsKeepTheirPlaceholderTokens() {
        val offline = Account(username = "Steve")
        assertFalse(offline.hasPlainSecrets())
        assertEquals(offline, offline.sealed())
    }
}
