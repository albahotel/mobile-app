package dev.rexe.hotelremote.bluetooth

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dev.rexe.hotelremote.compats.BluetoothManagerCompat
import dev.rexe.hotelremote.protofiles.Message
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

@Suppress("unused", "deprecation")
class BluetoothLEService : Service() {
    companion object {
        @JvmStatic
        private val LOG: String = "BluetoothLEService"

        const val ACTION_GATT_CONNECTED =
            "dev.rexe.rebadge.services.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED =
            "dev.rexe.rebadge.services.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED =
            "dev.rexe.rebadge.services.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_GATT_UPSTREAM =
            "dev.rexe.rebadge.services.ACTION_GATT_UP_STREAM"
        const val ACTION_DATA_AVAILABLE =
            "dev.rexe.rebadge.services.ACTION_DATA_AVAILABLE"

        const val BUNDLE_KEY_CHARACTERISTIC_UUID =
            "bundle.characteristic.uuid"
        const val BUNDLE_KEY_CHARACTERISTIC_BYTES_DATA =
            "bundle.characteristic.value"

        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTED = 2

        private val BLE_SERVICE_UUID = UUID.fromString("000000ff-0000-1000-8000-00805f9b34fb")
        private val BLE_IDENTIFY_CHAR_UUID = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb")
        private val BLE_MESSAGES_CHAR_UUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")
        private val BLE_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private var identifyToken: String? = ""
        private var identified: Boolean = false
    }

    data class WriteRequest(val bytes: ByteArray, val flags: Int) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as WriteRequest

            return bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            return bytes.contentHashCode()
        }
    }

    private val binder = LocalBinder()

    private var connectionState = STATE_DISCONNECTED

    private var bluetoothAdapter: BluetoothAdapter? = null

    private var bluetoothDevice: BluetoothDevice? = null
    private var bluetoothDeviceIdentifyCharacteristic: BluetoothGattCharacteristic? = null
    private var bluetoothDeviceReadCharacteristic: BluetoothGattCharacteristic? = null
    private var bluetoothDeviceWriteCharacteristic: BluetoothGattCharacteristic? = null

    private var writeQueue: ConcurrentLinkedQueue<WriteRequest> = ConcurrentLinkedQueue<WriteRequest>()
    private var isWriteBusy: Boolean = false

    private var bluetoothGatt: BluetoothGatt? = null
    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = STATE_CONNECTED
                broadcastUpdate(ACTION_GATT_CONNECTED)
                if (ActivityCompat.checkSelfPermission(this@BluetoothLEService, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothGatt?.discoverServices()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = STATE_DISCONNECTED
                broadcastUpdate(ACTION_GATT_DISCONNECTED)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                this@BluetoothLEService.setRWCharacteristics(gatt)

                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.w(LOG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val extras = Bundle()
                extras.putByteArray(BUNDLE_KEY_CHARACTERISTIC_BYTES_DATA, value)
                extras.putString(BUNDLE_KEY_CHARACTERISTIC_UUID, characteristic.uuid.toString())
                broadcastUpdate(ACTION_DATA_AVAILABLE, extras)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val extras = Bundle()
                if (characteristic?.uuid?.equals(BLE_IDENTIFY_CHAR_UUID) == true) {
                    identified = true
                    setRWCharacteristics(gatt)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            val extras = Bundle()

            if (!writeQueue.isEmpty()) {
                Log.d(LOG, "Next")
                val element = writeQueue.poll()
                if (element != null) {
                    writeDefaultCharacteristic(element.bytes, element.flags)
                }
            } else {
                Log.d(LOG, "End")
                isWriteBusy = false
            }

            extras.putByteArray(BUNDLE_KEY_CHARACTERISTIC_BYTES_DATA, value)
            extras.putString(BUNDLE_KEY_CHARACTERISTIC_UUID, characteristic.uuid.toString())
            broadcastUpdate(ACTION_DATA_AVAILABLE, extras)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if(descriptor?.characteristic == bluetoothDeviceReadCharacteristic) {
                Log.d(LOG, "writing read characteristic descriptor finished, status=$status");
                if (status != BluetoothGatt.GATT_SUCCESS) {

                } else {
                    broadcastUpdate(ACTION_GATT_UPSTREAM)
                    Log.d(LOG, "Upstream")
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    fun initialize(): Boolean {
        bluetoothAdapter = BluetoothManagerCompat.getAdapter(bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager)
        if (bluetoothAdapter == null) {
            Log.e(LOG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }

    fun connect(address: String, token: String): Boolean {
        identifyToken = token
        if (bluetoothAdapter != null)
            try {
                bluetoothDevice = bluetoothAdapter!!.getRemoteDevice(address)
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        bluetoothGatt = bluetoothDevice?.connectGatt(
                            this,
                            true,
                            bluetoothGattCallback,
                            BluetoothDevice.TRANSPORT_LE
                        )
                    else
                        bluetoothGatt = bluetoothDevice?.connectGatt(
                            this,
                            true,
                            bluetoothGattCallback,
                        )

                    identified = false
                } else
                    return false
                return true
            } catch (exception: IllegalArgumentException) {
                Log.w(LOG, "Device not found with provided address.")
                return false
            }

        return false
    }

    fun disconnect() {
        bluetoothGatt?.let { gatt ->
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                gatt.close()
            }
            bluetoothGatt = null
        }
    }

    fun getSupportedGattServices(): List<BluetoothGattService?>? {
        return bluetoothGatt?.services
    }

    override fun onUnbind(intent: Intent?): Boolean {
        disconnect()
        return super.onUnbind(intent)
    }

    private fun broadcastUpdate(action: String, extras: Bundle? = null) {
        val intent = Intent(action)
        if (extras != null)
            intent.putExtras(extras)
        sendBroadcast(intent)
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let { gatt ->
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                gatt.readCharacteristic(characteristic)
            }
        } ?: run {
            Log.w(LOG, "BluetoothGatt not initialized")
            return
        }
    }

    fun addToQueueForWriting(request: WriteRequest) {
        if (!isWriteBusy) {
            Log.d(LOG, "Begin")
            writeDefaultCharacteristic(request.bytes, request.flags)
            isWriteBusy = true
        } else
            writeQueue.offer(request)
    }

    fun writeDefaultCharacteristic(value: ByteArray, flags: Int) {
        bluetoothDeviceWriteCharacteristic?.let {
            this.writeCharacteristic(it, value, flags)
        }
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, value: ByteArray, flags: Int): Int {
        bluetoothGatt?.let { gatt ->
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    return gatt.writeCharacteristic(characteristic, value, flags)
                else {
                    characteristic.value = value
                    return if (gatt.writeCharacteristic(characteristic)) 0 else 0x7fffffff
                }
            }
        } ?: run {
            Log.w(LOG, "BluetoothGatt not initialized")
        }
        return BluetoothStatusCodes.ERROR_UNKNOWN
    }

    fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic, enabled: Boolean) {
        bluetoothGatt?.let { gatt ->
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                gatt.setCharacteristicNotification(characteristic, enabled)
            }
        } ?: run {
            Log.w(LOG, "BluetoothGatt not initialized")
        }
    }

    private fun setRWCharacteristics(gatt: BluetoothGatt?): Boolean {
        gatt?.let {
            gatt.services.forEach { service ->
                if (service.uuid.equals(BLE_SERVICE_UUID))
                    service.characteristics.forEach { char ->
                        Log.d("Bluetooth", "char uuid = " + char.uuid)

                        if (char.uuid.equals(BLE_IDENTIFY_CHAR_UUID) and !identified) {
                            Log.d("Bluetooth", "IDENTIFY")

                            bluetoothDeviceIdentifyCharacteristic = char

                            val writeProperties: Int? = char.properties
                            if(writeProperties != null &&
                                (writeProperties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
                                return false
                            }
                            if(ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                                return false

                            val b = Message.IdentifyRequest.newBuilder()
                            b.token = identifyToken
                            val id = b.build()

                            Log.d("Bluetooth", "status = " + writeCharacteristic(char, id.toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT))
                        } else if (char.uuid.equals(BLE_MESSAGES_CHAR_UUID) and identified) {
                            bluetoothDeviceReadCharacteristic = char
                            bluetoothDeviceWriteCharacteristic = char

                            val writeProperties: Int? = bluetoothDeviceWriteCharacteristic?.properties
                            if(writeProperties != null &&
                                (writeProperties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
                                return false
                            }
                            if(ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                                !gatt.setCharacteristicNotification(bluetoothDeviceReadCharacteristic, true)) {
                                return false
                            }

                            val readDescriptor: BluetoothGattDescriptor? = bluetoothDeviceReadCharacteristic?.getDescriptor(BLE_DESCRIPTOR_UUID)
                            if (readDescriptor == null) {
                                return false
                            }

                            val readProperties: Int? = bluetoothDeviceReadCharacteristic?.properties
                            val bytes = if (readProperties != null && (readProperties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                            } else if(readProperties != null && (readProperties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            } else {
                                return false
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                return gatt.writeDescriptor(readDescriptor, bytes) == BluetoothStatusCodes.SUCCESS
                            else {
                                readDescriptor.value = bytes
                                gatt.writeDescriptor(readDescriptor)
                            }
                        }
                    }
            }
        }
        return false
    }

    inner class LocalBinder : Binder() {
        fun getService() : BluetoothLEService {
            return this@BluetoothLEService
        }
    }
}