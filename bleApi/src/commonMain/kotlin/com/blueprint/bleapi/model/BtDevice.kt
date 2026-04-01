package com.blueprint.bleapi.model

data class BtDevice(val name: String, val address: String) {

    var rawData: Any? = null
        private set

    constructor(name: String, address: String, rawData: Any? = null) : this(name, address) {
        this.rawData = rawData
    }

}