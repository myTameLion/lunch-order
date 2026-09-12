package com.lunchorder.auth

import com.lunchorder.LunchProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PasswordVaultTest {

    private fun vault(secret: String = "") = PasswordVault(LunchProperties(exportKey = secret))

    @Test
    fun `加解密往返`() {
        val v = vault("my-secret-key")
        val enc = v.encrypt("abcdef88")
        assertEquals("abcdef88", v.decrypt(enc))
    }

    @Test
    fun `密文不含明文且每次随机`() {
        val v = vault("k")
        val e1 = v.encrypt("123456")
        val e2 = v.encrypt("123456")
        assert(!e1.contains("123456"))
        assert(e1 != e2) // 随机 IV
    }

    @Test
    fun `密钥不匹配时解密返回 null`() {
        val enc = vault("key-a").encrypt("123456")
        assertNull(vault("key-b").decrypt(enc))
    }

    @Test
    fun `空与损坏密文返回 null`() {
        assertNull(vault("k").decrypt(null))
        assertNull(vault("k").decrypt(""))
        assertNull(vault("k").decrypt("not-base64!!"))
    }
}
