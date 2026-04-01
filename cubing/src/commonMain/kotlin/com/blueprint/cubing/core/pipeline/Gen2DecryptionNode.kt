package com.blueprint.cubing.core.pipeline

import com.blueprint.cubing.core.crypto.GAN_ENCRYPTION_KEYS
import com.blueprint.cubing.core.crypto.GanCubeEncrypter
import com.blueprint.cubing.core.crypto.GanCubeEncrypterImpl
import com.blueprint.cubing.core.pipeline.base.PipelineNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class Gen2DecryptionNode(private val macAddress: String) : PipelineNode<ByteArray, ByteArray> {

    private val encrypter: GanCubeEncrypter = createEcrypter()

    override suspend fun apply(inputFlow: Flow<ByteArray>): Flow<ByteArray> {
        return inputFlow.map { encrypter.decrypt(it) }
    }

    private fun createEcrypter(): GanCubeEncrypter {
        val key = GAN_ENCRYPTION_KEYS[0].key.map { it.toByte() }.toByteArray()
        val iv = GAN_ENCRYPTION_KEYS[0].iv.map { it.toByte() }.toByteArray()

        val parts = macAddress.split(Regex("[:\\-\\s]+"))

        val salt = parts.map{ it.toUByte(16).toByte() }.reversed().toByteArray()
        return GanCubeEncrypterImpl(key, iv, salt)
    }

}