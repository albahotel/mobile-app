package dev.rexe.hotelremote.remote

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginTop
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.rexe.hotelremote.R
import dev.rexe.hotelremote.remote.fragments.general.RemoteGeneralFragment
import dev.rexe.hotelremote.remote.fragments.schedule.RemoteScheduleFragment
import androidx.core.view.get
import androidx.fragment.app.viewModels
import dev.rexe.hotelremote.bluetooth.BluetoothLEService
import dev.rexe.hotelremote.managers.BluetoothDoorManager
import dev.rexe.hotelremote.remote.fragments.requests.RemoteRequestsFragment

class RemoteActivity : AppCompatActivity() {
    var toolbar: MaterialToolbar? = null
    var bluetoothLEService: BluetoothLEService? = null

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            Log.d("Main", "Service binded")

            bluetoothLEService = (service as BluetoothLEService.LocalBinder).getService()
            bluetoothLEService?.let { bluetooth ->
                if (!bluetooth.initialize()) {
                    Log.e("Main", "Unable to initialize Bluetooth")
                    finish()
                }
                // perform device connection
                bluetooth.connect("C0:BB:CC:DD:EE:22", "gG55zXtXrdThTl9y")

                BluetoothDoorManager.bluetoothLEService = bluetooth
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
                }
                BluetoothLEService.ACTION_GATT_DISCONNECTED -> {
                    Log.d("Main", "Disconnected")
                }
                BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    Log.d("Main", "Discovered")
                }
                BluetoothLEService.ACTION_GATT_UPSTREAM -> {
                    Log.d("Main", "Upstreamed!")
                }
                BluetoothLEService.ACTION_DATA_AVAILABLE -> {
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
                .replace(R.id.container, RemoteGeneralFragment.newInstance())
                .commitNow()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.remote_bar_menu, menu)
        return true
    }
}