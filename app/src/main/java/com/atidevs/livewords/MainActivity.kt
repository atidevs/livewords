package com.atidevs.livewords

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.atidevs.livewords.livebasedtranslation.LiveTranslateFragment
import com.atidevs.livewords.splash.SplashFragment
import com.atidevs.livewords.splash.SplashFragmentDirections
import com.atidevs.livewords.textbasedtranslation.TextTranslateFragment

class MainActivity : AppCompatActivity() {

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                (navHostFragment?.childFragmentManager?.fragments?.get(0) as? SplashFragment)?.handlePermissionResult(
                    isGranted
                )
            }
    }

    fun launchPermissionDialog() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun checkAppPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                navigateToLiveTranslationScreen()
            }
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_DENIED -> {
                // Check if educational UI was already displayed to user!
                val educationalUIAlreadyShown = false
                if (educationalUIAlreadyShown) {
                    navigateToTextTranslationScreen()
                }
            }
        }
    }

    // Navigate to Text Translation screen
    private fun navigateToTextTranslationScreen() {
        findNavController(R.id.nav_host_fragment).navigate(
            SplashFragmentDirections.actionSplashFragmentToTextTranslateFragment()
        )
    }

    // Navigate to Live Translation screen
    private fun navigateToLiveTranslationScreen() {
        findNavController(R.id.nav_host_fragment).navigate(
            SplashFragmentDirections.actionSplashFragmentToLiveTranslateFragment()
        )
    }
}