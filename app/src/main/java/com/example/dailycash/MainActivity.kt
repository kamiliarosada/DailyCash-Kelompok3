package com.example.dailycash

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.dailycash.databinding.ActivityMainBinding

import com.example.dailycash.utils.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Terapkan tema sebelum onCreate
        val preferenceManager = PreferenceManager(this)
        preferenceManager.applyDarkMode(preferenceManager.isDarkMode())

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Pastikan menu bawah terhubung dengan NavController
        binding.bottomNavigation.setupWithNavController(navController)

        // Force navigation to handle cases where setupWithNavController might be inconsistent
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.navigation_dashboard) {
                navController.popBackStack(R.id.navigation_dashboard, false)
            } else if (item.itemId != navController.currentDestination?.id) {
                androidx.navigation.ui.NavigationUI.onNavDestinationSelected(item, navController)
            }
            true
        }
    }
}
