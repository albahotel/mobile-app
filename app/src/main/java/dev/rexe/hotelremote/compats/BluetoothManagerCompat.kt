package dev.rexe.hotelremote.compats

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.os.Build

object BluetoothManagerCompat {
    @JvmStatic
    @Suppress("ObsoleteSdkInt", "deprecation")
    fun getAdapter(bluetoothManager: BluetoothManager): BluetoothAdapter? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            bluetoothManager.adapter
        } else {
            BluetoothAdapter.getDefaultAdapter()
        }
    }
}