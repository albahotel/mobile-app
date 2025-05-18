package dev.rexe.hotelremote.remote

import Message
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.rexe.hotelremote.R
import dev.rexe.hotelremote.bluetooth.BluetoothLEService
import dev.rexe.hotelremote.managers.BluetoothDoorManager
import dev.rexe.hotelremote.remote.fragments.auth.RemoteAuthorizationFragment
import dev.rexe.hotelremote.remote.fragments.general.RemoteGeneralFragment
import dev.rexe.hotelremote.remote.fragments.requests.RemoteRequestsFragment
import dev.rexe.hotelremote.remote.fragments.schedule.RemoteScheduleFragment
import dev.rexe.hotelremote.scanner.QRScannerActivity
import java.io.File

class RemoteActivity : AppCompatActivity() {
    companion object {
        val QR_SCANNER_LAUNCH = 10
    }

    var toolbar: MaterialToolbar? = null
    var bluetoothLEService: BluetoothLEService? = null

    var authExists = false
    var broadcastReceiver: BroadcastReceiver? = null

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            Log.d("Main", "Service binded")

            bluetoothLEService = (service as BluetoothLEService.LocalBinder).getService()
            bluetoothLEService?.let { bluetooth ->
                if (!bluetooth.initialize()) {
                    Log.e("Main", "Unable to initialize Bluetooth")
                    finish()
                }

                BluetoothDoorManager.bluetoothLEService = bluetooth
                BluetoothDoorManager.connect()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.d("Main", "Service disconnected")
            bluetoothLEService = null
        }
    }

    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLEService.ACTION_GATT_CONNECTED -> {
                    Log.d("Main", "Connected")
                    Toast.makeText(context, "Connected!", Toast.LENGTH_SHORT).show()
                    BluetoothDoorManager.connected = true
                }
                BluetoothLEService.ACTION_GATT_DISCONNECTED -> {
                    Log.d("Main", "Disconnected")
                    BluetoothDoorManager.connected = false
                }
                BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    BluetoothDoorManager.fullDataInFields()
                    Log.d("Main", "Discovered")
                }
                BluetoothLEService.ACTION_GATT_UPSTREAM -> {
                    Log.d("Main", "Upstreamed!")
                }
                BluetoothLEService.ACTION_DATA_AVAILABLE -> {
                    val bytes = intent.extras?.getByteArray(BluetoothLEService.BUNDLE_KEY_CHARACTERISTIC_BYTES_DATA)
                    bytes?.size?.let {
                        if (it > 0) {
                            try {
                                val state = Message.State.parseFrom(bytes)
                                Log.d("STATE", "${state.lightOnValue}")
                            } catch (ignored: Exception) {

                            }
                        }
                    }
                    Log.d("Main", "Some data available!")
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_remote)
        toolbar = findViewById(R.id.remote_toolbar)
        setSupportActionBar(toolbar)

        val gattServiceIntent = Intent(this, BluetoothLEService::class.java)
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE)
        ContextCompat.registerReceiver(this, gattUpdateReceiver, IntentFilter().apply {
            addAction(BluetoothLEService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLEService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED)
            addAction(BluetoothLEService.ACTION_GATT_UPSTREAM)
            addAction(BluetoothLEService.ACTION_DATA_AVAILABLE)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)

        BluetoothDoorManager.getSharedPreferences(this)

        ViewCompat.setOnApplyWindowInsetsListener(toolbar!!, { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val mlp = view.layoutParams as ViewGroup.MarginLayoutParams

            mlp.setMargins(0, systemBars.top, 0, 0)
            view.layoutParams = mlp

            insets
        })

        findViewById<BottomNavigationView>(R.id.remote_navigation_bar).setOnItemSelectedListener { item ->
            toolbar?.title = item.title

            when(item.itemId) {
                R.id.item_general -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, RemoteGeneralFragment.newInstance())
                        .commitNow()

                    true
                }
                R.id.item_schedule -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, RemoteScheduleFragment.newInstance())
                        .commitNow()

                    true
                }
                R.id.item_requests -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, RemoteRequestsFragment.newInstance())
                        .commitNow()

                    true
                }
                else -> TODO()
            }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, if (File(applicationContext.filesDir.parent, "shared_prefs/auth.xml").exists())
                    RemoteGeneralFragment.newInstance() else RemoteAuthorizationFragment.newInstance())
                .commitNow()
        }

        authExists = File(applicationContext.filesDir.parent, "shared_prefs/auth.xml").exists()
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(arg0: Context?, intent: Intent) {
                val action = intent.getAction()
                if (action == "finish_activity") {
                    authExists = File(applicationContext.filesDir.parent, "shared_prefs/auth.xml").exists()
                    findViewById<BottomNavigationView>(R.id.remote_navigation_bar).isVisible = authExists
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, RemoteGeneralFragment.newInstance())
                        .commitNowAllowingStateLoss()
                    BluetoothDoorManager.connect()
                }
            }
        }
        ContextCompat.registerReceiver(applicationContext, broadcastReceiver, IntentFilter("finish_activity"),
            ContextCompat.RECEIVER_NOT_EXPORTED)

        findViewById<BottomNavigationView>(R.id.remote_navigation_bar).isVisible = File(applicationContext.filesDir.parent, "shared_prefs/auth.xml").exists()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.remote_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.item_qr_scan) {
            val intent: Intent = Intent(applicationContext, QRScannerActivity::class.java)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("LOG", "RESULT")
    }
}