package com.blueprint.androidapp.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.blueprint.bleapi.model.BtDevice
import com.blueprint.androidapp.impl.getRawData
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.coroutines.resume


class ConnectionManager(val context: Context) {


    private val btManager = context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

    @RequiresApi(Build.VERSION_CODES.R)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun getPairedDevices(): List<BtDevice> {
        val pairedDevices = btManager.adapter.bondedDevices
        return pairedDevices.map { com.blueprint.androidapp.impl.BtDevice(it) }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun getConnectedDevices(): List<BtDevice> {
        return getPairedDevices().filter { isConnected(it.getRawData()!!) }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun disconnect(device: BtDevice) {
        val data = device.getRawData() ?: return

        disconnectA2DP(data)
        disconnectHFP(data)
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun disconnectA2DP(device: BluetoothDevice) = suspendCancellableCoroutine { continuation ->
        // Fallback: try profile disconnect (A2DP) via getProfileProxy + reflection (may not work)
        btManager.adapter?.getProfileProxy(
            /* context = */ context,
            /* listener = */ object : BluetoothProfile.ServiceListener {

                @SuppressLint("DiscouragedPrivateApi")
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.A2DP) {
                        val disconnect: Method = try {
                            BluetoothA2dp::class.java.getDeclaredMethod(
                                "disconnect",
                                BluetoothDevice::class.java
                            )
                        } catch (e: NoSuchMethodException) {
                            e.printStackTrace()
                            return
                        }
                        disconnect.isAccessible = true
                        try {
                            disconnect.invoke(proxy, device)
                        } catch (e: IllegalAccessException) {
                            e.printStackTrace()
                        } catch (e: InvocationTargetException) {
                            e.printStackTrace()
                        }
                    }

                    btManager.adapter?.closeProfileProxy(profile, proxy)
                    continuation.resume(Unit)
                }

                override fun onServiceDisconnected(profile: Int) {}
            },
            /* profile = */ BluetoothProfile.A2DP,
        )

    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun disconnectHFP(device: BluetoothDevice) = suspendCancellableCoroutine { continuation ->
        // Fallback: try profile disconnect (A2DP) via getProfileProxy + reflection (may not work)
        btManager.adapter?.getProfileProxy(
            /* context = */ context,
            /* listener = */ object : BluetoothProfile.ServiceListener {

                @SuppressLint("DiscouragedPrivateApi")
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.HEADSET) {
                        val disconnect: Method = try {
                            BluetoothHeadset::class.java.getDeclaredMethod(
                                "disconnect",
                                BluetoothDevice::class.java
                            )
                        } catch (e: NoSuchMethodException) {
                            e.printStackTrace()
                            return
                        }
                        disconnect.isAccessible = true
                        try {
                            disconnect.invoke(proxy, device)
                        } catch (e: IllegalAccessException) {
                            e.printStackTrace()
                        } catch (e: InvocationTargetException) {
                            e.printStackTrace()
                        }
                    }

                    btManager.adapter?.closeProfileProxy(profile, proxy)
                    continuation.resume(Unit)
                }

                override fun onServiceDisconnected(profile: Int) {}
            },
            /* profile = */ BluetoothProfile.HEADSET,
        )

    }



    private fun isConnected(device: BluetoothDevice): Boolean {
        return try {
            val m: Method = device.javaClass.getMethod("isConnected")
            m.invoke(device) as Boolean
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }

}
