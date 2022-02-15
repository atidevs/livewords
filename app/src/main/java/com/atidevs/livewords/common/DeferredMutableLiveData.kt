package com.atidevs.livewords.common

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData

/**
 * This class helps delay the setter for the MutableLiveData for a specific amount of time
 * In particular this is very useful for catching 'values' coming from a camera feed
 * after the feed has been stable for a certain amount of time
 */
class DeferredMutableLiveData<T>(private val delay: Long) : MutableLiveData<T>() {

    private var pendingValue: T? = null
    private val runnable: Runnable = Runnable {
        super.setValue(pendingValue)
    }

    override fun setValue(value: T) {
        if (value != pendingValue) {
            pendingValue = value
            Handler(Looper.getMainLooper()).removeCallbacks(runnable)
            Handler(Looper.getMainLooper()).postDelayed(runnable, delay)
        }
    }
}