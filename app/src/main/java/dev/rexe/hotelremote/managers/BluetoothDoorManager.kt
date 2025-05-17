package dev.rexe.hotelremote.managers

import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import dev.rexe.hotelremote.bluetooth.BluetoothLEService
import dev.rexe.hotelremote.protofiles.Message

object BluetoothDoorManager {
    @JvmField
    var bluetoothLEService: BluetoothLEService? = null

    private var lightStatus: Boolean = false
    private var doorLockStatus: Boolean = true
    private var nextLock: Int = 0

    fun setDoorLockStatus(locked: Boolean) {
        if (bluetoothLEService != null) {
            val b = Message.ClientMessage.newBuilder()
            val stB = Message.SetState.newBuilder()
            stB.setState(if (locked) Message.States.DoorLockOpen else Message.States.DoorLockClose)
            b.setSetState(stB)
            val cm = stB.build()

            nextLock++

            doorLockStatus = locked

            bluetoothLEService?.addToQueueForWriting(BluetoothLEService.WriteRequest(cm.toByteArray(),
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT))
        }
    }

    fun getDoorLockStatus(): Boolean {
        return doorLockStatus
    }
}