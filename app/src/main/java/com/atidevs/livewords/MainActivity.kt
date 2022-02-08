package com.atidevs.livewords

import android.Manifest
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var translateFragment: TranslateFragment
    private lateinit var splashFragment: SplashFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            translateFragment = TranslateFragment()
            splashFragment = SplashFragment()
        }

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                //splashFragment.handlePermission(isGranted)
            }

        launchPermissionDialog()

        // Attach Translate fragment to the activity!
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, splashFragment, "Translate")
            .commitAllowingStateLoss()
    }

    fun launchPermissionDialog() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
}