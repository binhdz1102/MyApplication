package com.example.myapplication.data.remote.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import com.example.myapplication.data.remote.aidl.IControlPanelCallback
import com.example.myapplication.data.remote.aidl.IControlPanelService

class ControlPanelAidlService : Service() {

    private val callbacks = RemoteCallbackList<IControlPanelCallback>()

    @Volatile
    private var rating = INITIAL_RATING

    @Volatile
    private var temperature = INITIAL_TEMPERATURE

    @Volatile
    private var toggledOn = INITIAL_TOGGLED_ON

    private val binder = object : IControlPanelService.Stub() {
        override fun getRating(): Int = synchronized(this@ControlPanelAidlService) {
            Log.d(TAG, "getRating() -> $rating")
            rating
        }

        override fun getTemperature(): Int = synchronized(this@ControlPanelAidlService) {
            Log.d(TAG, "getTemperature() -> $temperature")
            temperature
        }

        override fun isToggledOn(): Boolean = synchronized(this@ControlPanelAidlService) {
            Log.d(TAG, "isToggledOn() -> $toggledOn")
            toggledOn
        }

        override fun decreaseRating() {
            Log.d(TAG, "decreaseRating() requested")
            changeRating(-1)
        }

        override fun increaseRating() {
            Log.d(TAG, "increaseRating() requested")
            changeRating(1)
        }

        override fun decreaseTemperature() {
            Log.d(TAG, "decreaseTemperature() requested")
            changeTemperature(-1)
        }

        override fun increaseTemperature() {
            Log.d(TAG, "increaseTemperature() requested")
            changeTemperature(1)
        }

        override fun setToggledOn(toggledOn: Boolean) {
            Log.d(TAG, "setToggledOn($toggledOn) requested")
            updateToggle(toggledOn)
        }

        override fun registerCallback(callback: IControlPanelCallback?) {
            if (callback == null) {
                Log.w(TAG, "registerCallback(null) ignored")
                return
            }
            Log.d(TAG, "registerCallback(${callback.hashCode()})")
            callbacks.register(callback)
            dispatchCurrentState(callback)
        }

        override fun unregisterCallback(callback: IControlPanelCallback?) {
            if (callback == null) {
                Log.w(TAG, "unregisterCallback(null) ignored")
                return
            }
            Log.d(TAG, "unregisterCallback(${callback.hashCode()})")
            callbacks.unregister(callback)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate() state=${snapshotState()}")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand(startId=$startId, flags=$flags, intent=$intent) state=${snapshotState()}")
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind(intent=$intent) state=${snapshotState()}")
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind(intent=$intent) state=${snapshotState()}")
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent) {
        super.onRebind(intent)
        Log.d(TAG, "onRebind(intent=$intent) state=${snapshotState()}")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() state=${snapshotState()}")
        callbacks.kill()
        super.onDestroy()
    }

    private fun changeRating(delta: Int) {
        val before = snapshotState()
        val changed = synchronized(this) {
            val boundedRating = (rating + delta).coerceIn(0, MAX_RATING)
            if (boundedRating == rating) {
                false
            } else {
                rating = boundedRating
                true
            }
        }
        if (changed) {
            Log.d(TAG, "changeRating(delta=$delta) $before -> ${snapshotState()}")
            broadcastState()
        } else {
            Log.d(TAG, "changeRating(delta=$delta) ignored, state=$before")
        }
    }

    private fun changeTemperature(delta: Int) {
        val before = snapshotState()
        val changed = synchronized(this) {
            val newTemperature = temperature + delta
            if (newTemperature == temperature) {
                false
            } else {
                temperature = newTemperature
                true
            }
        }
        if (changed) {
            Log.d(TAG, "changeTemperature(delta=$delta) $before -> ${snapshotState()}")
            broadcastState()
        } else {
            Log.d(TAG, "changeTemperature(delta=$delta) ignored, state=$before")
        }
    }

    private fun updateToggle(newToggledOn: Boolean) {
        val before = snapshotState()
        val changed = synchronized(this) {
            if (newToggledOn == toggledOn) {
                false
            } else {
                toggledOn = newToggledOn
                true
            }
        }
        if (changed) {
            Log.d(TAG, "updateToggle($newToggledOn) $before -> ${snapshotState()}")
            broadcastState()
        } else {
            Log.d(TAG, "updateToggle($newToggledOn) ignored, state=$before")
        }
    }

    private fun dispatchCurrentState(callback: IControlPanelCallback) {
        val snapshot = snapshotState()
        Log.d(TAG, "dispatchCurrentState(callback=${callback.hashCode()}, state=$snapshot)")
        try {
            callback.onStateChanged(snapshot.rating, snapshot.temperature, snapshot.toggledOn)
        } catch (exception: RemoteException) {
            Log.w(TAG, "Unable to dispatch current state to callback", exception)
        }
    }

    private fun broadcastState() {
        val snapshot = snapshotState()
        val count = callbacks.beginBroadcast()
        Log.d(TAG, "broadcastState(count=$count, state=$snapshot)")
        try {
            for (index in 0 until count) {
                try {
                    callbacks.getBroadcastItem(index)
                        .onStateChanged(snapshot.rating, snapshot.temperature, snapshot.toggledOn)
                } catch (exception: RemoteException) {
                    Log.w(TAG, "Unable to dispatch state update to callback #$index", exception)
                }
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }

    private fun snapshotState(): ServiceState = synchronized(this) {
        ServiceState(
            rating = rating,
            temperature = temperature,
            toggledOn = toggledOn,
        )
    }

    private data class ServiceState(
        val rating: Int,
        val temperature: Int,
        val toggledOn: Boolean,
    ) {
        override fun toString(): String {
            return "ServiceState(rating=$rating, temperature=$temperature, toggledOn=$toggledOn)"
        }
    }

    companion object {
        private const val TAG = "ControlPanelService"
        private const val INITIAL_RATING = 5
        private const val INITIAL_TEMPERATURE = 10
        private const val INITIAL_TOGGLED_ON = false
        private const val MAX_RATING = 5
    }
}
