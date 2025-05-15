package dev.rexe.hotelremote

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.rexe.hotelremote.ui.management.ManagementFragment
import dev.rexe.hotelremote.ui.plan.PlanFragment
import dev.rexe.hotelremote.ui.requests.RequestsFragment

class RemoteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_remote)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_navigation)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ManagementFragment.newInstance())
                .commitNow()
        }
        findViewById<BottomNavigationView>(R.id.bottom_navigation).setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.item_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, ManagementFragment.newInstance())
                        .commitNow()
                    true
                }
                R.id.item_schedule -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, PlanFragment.newInstance())
                        .commitNow()
                    true
                }
                R.id.item_requests -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, RequestsFragment.newInstance())
                        .commitNow()
                    true
                }
                else -> false
            }
        }
    }
}