package com.atidevs.livewords.splash

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.atidevs.livewords.MainActivity
import com.atidevs.livewords.common.Destination
import com.atidevs.livewords.databinding.FragmentSplashBinding

class SplashFragment : Fragment() {

    private val binding get() = _binding!!

    private var _binding: FragmentSplashBinding? = null

    private var splashViewModel: SplashViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        splashViewModel = ViewModelProvider(this)[SplashViewModel::class.java]
        binding.viewModel = splashViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        (requireActivity() as? MainActivity)?.checkAppPermission()
        //splashViewModel?.init()
    }

    private fun initViews() {
        binding.attribution.movementMethod = LinkMovementMethod.getInstance()
        binding.actionEnablePermission.setOnClickListener {
            (requireActivity() as? MainActivity)?.launchPermissionDialog()
        }

        splashViewModel?.showLoading?.observe(viewLifecycleOwner) { shouldShowLoading ->
            if (shouldShowLoading) {
                binding.loading.visibility = View.VISIBLE
                binding.loading.animate()
            } else {
                binding.loading.visibility = View.GONE
                binding.loading.animation?.cancel()
            }
        }

        splashViewModel?.isPermissionGranted?.observe(viewLifecycleOwner) { isGranted ->
            if (isGranted) {
                navigateToLiveTranslationScreen()
            } else {
                navigateToTextTranslationScreen()
            }
        }

        splashViewModel?.navigateTo?.observe(viewLifecycleOwner) {
            when (it) {
                Destination.TextTranslationScreen -> {
                    navigateToTextTranslationScreen()
                }
                Destination.LiveTranslationScreen -> {
                    navigateToLiveTranslationScreen()
                }
                else -> {}
            }
        }
    }

    // Navigate to Text Translation screen
    private fun navigateToTextTranslationScreen() {
        findNavController().navigate(
            SplashFragmentDirections.actionSplashFragmentToTextTranslateFragment()
        )
    }

    // Navigate to Live Translation screen
    private fun navigateToLiveTranslationScreen() {
        findNavController().navigate(
            SplashFragmentDirections.actionSplashFragmentToLiveTranslateFragment()
        )
    }

    fun handlePermissionResult(isGranted: Boolean) {
        splashViewModel?.handlePermissionResult(isGranted)
    }
}