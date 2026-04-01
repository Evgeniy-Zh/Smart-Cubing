package com.blueprint.cubing.core.crypto


/** Common cube encrypter interface */
interface GanCubeEncrypter {
    /** Encrypt binary message buffer represented as ByteArray */
    fun encrypt(data: ByteArray): ByteArray
    /** Decrypt binary message buffer represented as ByteArray */
    fun decrypt(data: ByteArray): ByteArray
}

/** Implementation for encryption scheme used in the GAN Gen2 Smart Cubes */
expect class GanCubeEncrypterImpl(key: ByteArray, iv: ByteArray, salt: ByteArray) : GanCubeEncrypter {
    override fun encrypt(data: ByteArray): ByteArray
    override fun decrypt(data: ByteArray): ByteArray
}
