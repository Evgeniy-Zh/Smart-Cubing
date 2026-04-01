package com.blueprint.cubing.core.crypto

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

actual class GanCubeEncrypterImpl actual constructor(key: ByteArray, iv: ByteArray, salt: ByteArray) : GanCubeEncrypter {

    protected val _key: ByteArray
    protected val _iv: ByteArray

    init {
        require(key.size == 16) { "Key must be 16 bytes (128-bit) long" }
        require(iv.size == 16) { "Iv must be 16 bytes (128-bit) long" }
        require(salt.size == 6) { "Salt must be 6 bytes (48-bit) long" }

        // Apply salt to key and iv (port of TS implementation: (key[i] + salt[i]) % 0xFF)
        val k = key.copyOf()
        val v = iv.copyOf()
        for (i in 0 until 6) {
            // follow TS math: % 0xFF (255)
            val kb = (k[i].toInt() and 0xFF)
            val sb = (salt[i].toInt() and 0xFF)
            k[i] = ((kb + sb) % 0xFF).toByte()

            val ib = (v[i].toInt() and 0xFF)
            v[i] = ((ib + sb) % 0xFF).toByte()
        }
        _key = k
        _iv = v
    }

    private fun encryptChunk(buffer: ByteArray, offset: Int) {
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        val keySpec = SecretKeySpec(_key, "AES")
        val ivSpec = IvParameterSpec(_iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val chunk = cipher.doFinal(buffer, offset, 16)
        System.arraycopy(chunk, 0, buffer, offset, 16)
    }

    private fun decryptChunk(buffer: ByteArray, offset: Int) {
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        val keySpec = SecretKeySpec(_key, "AES")
        val ivSpec = IvParameterSpec(_iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val chunk = cipher.doFinal(buffer, offset, 16)
        System.arraycopy(chunk, 0, buffer, offset, 16)
    }

    actual override fun encrypt(data: ByteArray): ByteArray {
        require(data.size >= 16) { "Data must be at least 16 bytes long" }
        val res = data.copyOf()
        // encrypt 16-byte chunk aligned to message start
        encryptChunk(res, 0)
        // encrypt 16-byte chunk aligned to message end
        if (res.size > 16) {
            encryptChunk(res, res.size - 16)
        }
        return res
    }

    actual override fun decrypt(data: ByteArray): ByteArray {
        require(data.size >= 16) { "Data must be at least 16 bytes long" }
        val res = data.copyOf()
        // decrypt 16-byte chunk aligned to message end
        if (res.size > 16) {
            decryptChunk(res, res.size - 16)
        }
        // decrypt 16-byte chunk aligned to message start
        decryptChunk(res, 0)
        return res
    }
}

