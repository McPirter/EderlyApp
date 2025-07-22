package com.example.elderly

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

open class BaseActivity : AppCompatActivity() {

    open fun getLayoutId(): Int = R.layout.activity_dashboard
    open fun getNavItemId(): Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        val contentFrame = findViewById<FrameLayout>(R.id.content_frame)
        layoutInflater.inflate(getLayoutId(), contentFrame)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == getNavItemId()) {
                return@setOnItemSelectedListener true // ya estamos en esta activity
            }

            when (item.itemId) {
                R.id.nav_dashboard -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    true
                }
                R.id.nav_location -> {
                    val intent = Intent(this, UbicacionActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }


        // Marcar el ítem actual como seleccionado
        getNavItemId()?.let {
            bottomNav.selectedItemId = it
        }
    }
}
