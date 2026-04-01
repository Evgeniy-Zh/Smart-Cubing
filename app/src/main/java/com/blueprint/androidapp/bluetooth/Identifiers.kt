package com.blueprint.androidapp.bluetooth

import java.util.UUID

object Identifiers {
   val SERVICE_UUID: UUID = UUID.fromString("24fe0c46-56a5-428c-a7fd-67d2aba719f3")
   val COMMAND_CHARACTERISTIC_UUID: UUID = UUID.fromString("4cdc9d20-e3fb-4ed6-a55c-69b97a7c617c")
   val CONNECTED_DEVICES_CHARACTERISTIC_UUID: UUID = UUID.fromString("4370f554-a20d-44d7-aa97-e8373688a5f0")
}