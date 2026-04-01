package com.blueprint.cubing.core.model

sealed class ConnectionState {
    data object Connected : ConnectionState()
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Initializing: ConnectionState()
    data object Disconnecting: ConnectionState()
    data object FailedToConnect: ConnectionState()
}