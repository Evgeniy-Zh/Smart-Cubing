package com.blueprint.bleapi.model

enum class ConnectionState {
    CONNECTING,
    CONNECTED,
    SERVICES_DISCOVERED,
    NOTIFICATIONS_ENABLED,
    DISCONNECTING,
    DISCONNECTED,
    CONNECTION_FAILED,
}