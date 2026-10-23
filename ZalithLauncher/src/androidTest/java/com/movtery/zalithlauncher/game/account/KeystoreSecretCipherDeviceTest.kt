package com.movtery.zalithlauncher.game.account

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a real device: the Android Keystore cipher that protects account tokens must round-trip, use a fresh IV each time
 * and keep working with the key it created earlier (second instance = same alias). Uses its own alias and fake values only;
 * it never reads the accounts database.
 */
@RunWith(AndroidJUnit4::class)
class KeystoreSecretCipherDeviceTest {
    private val alias = "all_account_secrets_device_test"

    @Test
    fun roundTripAndFreshIvAndKeyReuse() {
        val first = KeystoreSecretCipher(alias)
        val plain = "FAKE-ACCESS-TOKEN-for-device-test".toByteArray()
        val a = first.encrypt(plain)
        val b = first.encrypt(plain)
        assertNotEquals("IV must differ per encryption", a.toList(), b.toList())
        assertFalse(String(a).contains("FAKE-ACCESS"))
        assertEquals(String(plain), String(first.decrypt(a)))
        //a new instance finds the key that already exists in the Keystore (this is what happens after an app restart)
        assertEquals(String(plain), String(KeystoreSecretCipher(alias).decrypt(b)))
    }

    @Test
    fun accountSecretsWithTheKeystoreCipher() {
        val old = AccountSecrets.cipher
        try {
            AccountSecrets.cipher = KeystoreSecretCipher(alias)
            val acc = Account(accessToken = "FAKE-AT-1", refreshToken = "FAKE-RT-2", username = "DeviceTest")
            val row = acc.sealed()
            assertTrue(row.accessToken.startsWith("enc1:") && row.refreshToken.startsWith("enc1:"))
            assertFalse(row.accessToken.contains("FAKE-AT-1"))
            assertEquals(acc, row.opened())
        } finally {
            AccountSecrets.cipher = old
        }
    }
}
