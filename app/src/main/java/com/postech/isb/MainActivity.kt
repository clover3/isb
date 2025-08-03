package com.postech.isb

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.postech.isb.databinding.ActivityMainBinding
import com.postech.isb.ui.compose.ComposeFragment
import com.postech.isb.ui.login.LoginFragment
import com.postech.isb.ui.mybbs.MyBBSFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        prefSetTheme() // Set theme before super.onCreate()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setCurrentFragment(LoginFragment())

        binding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_compose -> setCurrentFragment(ComposeFragment())
                R.id.nav_login -> setCurrentFragment(LoginFragment())
                R.id.nav_mybbs -> setCurrentFragment(MyBBSFragment())
            }
            true
        }

        // Set the default selected tab in the bottom navigation view
        binding.bottomNavigation.selectedItemId = R.id.nav_login
    }

    private fun setCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.nav_host_fragment, fragment)
            commit()
        }
    }

    // Migrated from IsbTabWidget.java
    private fun prefSetTheme() {
        val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val savedId = sp.getString("saved_id", "")
        if (savedId == "tester2" || savedId == "늄븁") {
            // Do nothing to keep the default theme
        } else {
            setTheme(R.style.MyNoTitleBar)
        }
    }

    // Migrated from IsbTabWidget.java
    override fun onDestroy() {
        super.onDestroy()
        // Safely access the application context and disconnect the session
        (applicationContext as? PostechIsb)?.isb?.disconnect()
    }
}
