package com.atidevs.livewords.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atidevs.livewords.common.Destination
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {

    private val _isPermissionGranted: MutableLiveData<Boolean> = MutableLiveData()
    val isPermissionGranted: LiveData<Boolean> = _isPermissionGranted

    private val _showLoading: MutableLiveData<Boolean> = MutableLiveData()
    val showLoading: LiveData<Boolean> = _showLoading

    private val _navigateTo: MutableLiveData<Destination> = MutableLiveData()
    val navigateTo: LiveData<Destination> = _navigateTo

    fun handlePermissionResult(isGranted: Boolean) {
        _isPermissionGranted.value = isGranted
    }

    fun navigateToTextBasedExperience() {
        _navigateTo.value = Destination.TextTranslationScreen
    }
}