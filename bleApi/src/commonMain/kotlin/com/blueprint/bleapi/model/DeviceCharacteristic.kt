
package com.blueprint.bleapi.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class Property {
    READ, WRITE, NOTIFY, INDICATE
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class DeviceCharacteristic(
    @Serializable(with = UUIDSerializer::class)
    val uuid: Uuid,
    @Serializable(with = UUIDSerializer::class)
    val serviceUuid: Uuid,
    val properties: List<Property> = emptyList(),
)


/**
 * Custom serializer for java.util.UUID to work with kotlinx.serialization
 */
@OptIn(ExperimentalUuidApi::class)
object UUIDSerializer : KSerializer<Uuid> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "UUID",
        PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: Uuid) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Uuid {
        return  Uuid.parse(decoder.decodeString())
    }
}
