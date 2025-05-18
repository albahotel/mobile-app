package dev.rexe.hotelremote.managers

import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import dev.rexe.hotelremote.bluetooth.BluetoothLEService
import dev.rexe.hotelremote.protofiles.Message

object BluetoothDoorManager {
    @JvmField
    var bluetoothLEService: BluetoothLEService? = null

    private var doorLockStatus: Boolean = true
    private var lightStatus: Boolean = false

    var connected = false

    var shr: SharedPreferences? = null

    fun getSharedPreferences(context: Context) {
        BluetoothDoorManager.shr = context.getSharedPreferences("auth", MODE_PRIVATE)
    }

    fun connect() {
        shr?.let {
            if (connected)
                bluetoothLEService?.disconnect()
            if (it.contains("doorMac"))
                bluetoothLEService?.connect(it.getString("doorMac", "")!!, it.getString("doorToken", "")!!)
        }
    }

    fun fullDataInFields() {
        if (bluetoothLEService != null) {
            val stB = Message.GetState.newBuilder()
            val cm = stB.build()

            bluetoothLEService?.addToQueueForWriting(BluetoothLEService.WriteRequest(cm.toByteArray(),
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT))
        }
    }

    fun setDoorLockStatus(locked: Boolean) {
        if (bluetoothLEService != null) {
            val stB = Message.SetState.newBuilder()
            stB.setState(if (locked) Message.States.DoorLockClose else Message.States.DoorLockOpen)
            val cm = stB.build()

            doorLockStatus = locked
            bluetoothLEService?.addToQueueForWriting(BluetoothLEService.WriteRequest(cm.toByteArray(),
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT))
        }
    }

    fun setLightStatus(lightOn: Boolean) {
        if (bluetoothLEService != null) {
            val stB = Message.SetState.newBuilder()
            stB.setState(if (lightOn) Message.States.LightOn else Message.States.LightOff)
            val cm = stB.build()

            lightStatus = lightOn
            bluetoothLEService?.addToQueueForWriting(BluetoothLEService.WriteRequest(cm.toByteArray(),
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT))
        }
    }

    fun getDoorLockStatus(): Boolean {
        return doorLockStatus
    }

    fun getLightStatus(): Boolean {
        return lightStatus
    }
}