package com.blueprint.cubing.core.serialize

import com.blueprint.bleapi.model.BtDevice
import com.blueprint.cubing.core.crypto.GanCubeEncrypterImpl
import com.blueprint.cubing.core.crypto.GAN_ENCRYPTION_KEYS
import com.blueprint.cubing.core.crypto.GanCubeEncrypter
import com.blueprint.cubing.core.model.CubeRequest
import com.blueprint.cubing.core.serialize.base.RequestSerializer

@OptIn(ExperimentalUnsignedTypes::class)
class Gen2RequestSerializer(val btDevice: BtDevice): RequestSerializer {

    private val macAddress = btDevice.address

    private val encrypter: GanCubeEncrypter by lazy {
        createEcrypter()
    }

    override fun serialize(cubeRequest: CubeRequest): ByteArray {
        return commandToMessage(cubeRequest)
    }


    fun commandToMessage(cubeRequest: CubeRequest): ByteArray {
        val msg = createCommandMessage(cubeRequest)
        return encrypter.encrypt(msg)
    }

    private fun createCommandMessage(cubeRequest: CubeRequest): ByteArray {
        var msg = ByteArray(20)
        val resetMsg by lazy {
            ubyteArrayOf(0x0Au, 0x05u, 0x39u, 0x77u, 0x00u, 0x00u, 0x01u, 0x23u, 0x45u, 0x67u, 0x89u, 0xABu, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u)
                .toByteArray()
        }

//            CubeCommand.REQUEST_FACELETS -> msg[0] = 0x04
//            CubeCommand.REQUEST_HARDWARE -> msg[0] = 0x05
//            CubeCommand.REQUEST_BATTERY -> msg[0] = 0x09
//            CubeCommand.REQUEST_RESET -> { msg = resetMsg }

        when(cubeRequest){
            CubeRequest.Sync -> {  //REQUEST_FACELETS
                msg[0] = 0x04
            }
            CubeRequest.Reset -> {  //REQUEST_RESET
                msg = resetMsg
            }
            CubeRequest.Battery -> TODO()
            CubeRequest.Hardware -> TODO()
            is CubeRequest.RawRequest -> TODO()
        }

        return msg
    }

    private fun createEcrypter(): GanCubeEncrypter {
        val key = GAN_ENCRYPTION_KEYS[0].key.map { it.toByte() }.toByteArray()
        val iv = GAN_ENCRYPTION_KEYS[0].iv.map { it.toByte() }.toByteArray()

        val parts = macAddress.split(Regex("[:\\-\\s]+"))

        val salt = parts.map{ it.toUByte(16).toByte() }.reversed().toByteArray()
        return GanCubeEncrypterImpl(key, iv, salt)
    }

}