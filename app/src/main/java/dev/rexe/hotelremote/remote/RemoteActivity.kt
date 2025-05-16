package dev.rexe.hotelremote.remote

import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginTop
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.rexe.hotelremote.R
import dev.rexe.hotelremote.remote.fragments.general.RemoteGeneralFragment
import dev.rexe.hotelremote.remote.fragments.schedule.RemoteScheduleFragment
import androidx.core.view.get

class RemoteActivity : AppCompatActivity() {
    var toolbar: MaterialToolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_remote)
        toolbar = findViewById(R.id.remote_toolbar)
        setSupportActionBar(toolbar)

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
                        .replace(R.id.container, RemoteScheduleFragment.newInstance())
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