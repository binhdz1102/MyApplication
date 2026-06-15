package com.example.myapplication.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.example.myapplication.data.remote.aidl.IControlPanelCallback
import com.example.myapplication.data.remote.aidl.IControlPanelService
import com.example.myapplication.data.remote.service.ControlPanelAidlService
import com.example.myapplication.domain.model.ControlPanelState
import com.example.myapplication.domain.repository.ControlPanelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class AidlControlPanelRepository @Inject constructor(
    @ApplicationContext context: Context,
) : ControlPanelRepository {

    private val mutableState = MutableStateFlow(ControlPanelState())

    override val controlState: StateFlow<ControlPanelState> = mutableState.asStateFlow()

    private val appContext = context.applicationContext
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val serviceIntent = Intent(appContext, ControlPanelAidlService::class.java)

    @Volatile
    private var controlService: IControlPanelService? = null

    @Volatile
    private var isBound = false

    private val callback = object : IControlPanelCallback.Stub() {
        override fun onStateChanged(rating: Int, temperature: Int, toggledOn: Boolean) {
            Log.d(
                TAG,
                "callback.onStateChanged(rating=$rating, temperature=$temperature, toggledOn=$toggledOn)",
            )
            mutableState.update {
                it.copy(
                    rating = rating.coerceIn(0, MAX_RATING),
                    temperature = temperature,
                    toggledOn = toggledOn,
                    isServiceConnected = true,
                )
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "onServiceConnected(name=$name, binder=$service)")
            controlService = IControlPanelService.Stub.asInterface(service)
            isBound = true

            repositoryScope.launch {
                val connectedService = controlService ?: return@launch
                runCatching {
                    Log.d(TAG, "Registering callback with AIDL service")
                    connectedService.registerCallback(callback)
                    refreshState(connectedService)
                }.onFailure { exception ->
                    Log.e(TAG, "Unable to register AIDL callback", exception)
                    mutableState.update { it.copy(isServiceConnected = false) }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "onServiceDisconnected(name=$name)")
            controlService = null
            isBound = false
            mutableState.update { it.copy(isServiceConnected = false) }
        }

        override fun onBindingDied(name: ComponentName?) {
            Log.w(TAG, "onBindingDied(name=$name)")
            controlService = null
            isBound = false
            mutableState.update { it.copy(isServiceConnected = false) }
        }

        override fun onNullBinding(name: ComponentName?) {
            Log.w(TAG, "onNullBinding(name=$name)")
            controlService = null
            isBound = false
            mutableState.update { it.copy(isServiceConnected = false) }
        }
    }

    override fun connect() {
        if (isBound) {
            Log.d(TAG, "connect() ignored because service is already bound")
            return
        }

        Log.d(TAG, "connect() startService + bindService")
        runCatching {
            appContext.startService(serviceIntent)
        }.onFailure { exception ->
            Log.e(TAG, "Unable to start control panel service", exception)
        }

        val didBind = appContext.bindService(
            serviceIntent,
            serviceConnection,
            Context.BIND_AUTO_CREATE,
        )
        Log.d(TAG, "bindService() result=$didBind")

        if (!didBind) {
            mutableState.update { it.copy(isServiceConnected = false) }
            Log.e(TAG, "Unable to bind to control panel service")
        }
    }

    override fun disconnect() {
        if (!isBound) {
            Log.d(TAG, "disconnect() ignored because service is not bound")
            return
        }

        Log.d(TAG, "disconnect() unregisterCallback + unbindService")
        runCatching {
            controlService?.unregisterCallback(callback)
        }.onFailure { exception ->
            Log.w(TAG, "Unable to unregister AIDL callback cleanly", exception)
        }

        runCatching {
            appContext.unbindService(serviceConnection)
        }.onFailure { exception ->
            Log.w(TAG, "Unable to unbind from control panel service cleanly", exception)
        }

        controlService = null
        isBound = false
        mutableState.update { it.copy(isServiceConnected = false) }
    }

    override suspend fun decreaseRating() {
        invokeRemote("decreaseRating") { decreaseRating() }
    }

    override suspend fun increaseRating() {
        invokeRemote("increaseRating") { increaseRating() }
    }

    override suspend fun decreaseTemperature() {
        invokeRemote("decreaseTemperature") { decreaseTemperature() }
    }

    override suspend fun increaseTemperature() {
        invokeRemote("increaseTemperature") { increaseTemperature() }
    }

    override suspend fun toggleCenterButton() {
        val nextToggleState = !mutableState.value.toggledOn
        invokeRemote("setToggledOn($nextToggleState)") { setToggledOn(nextToggleState) }
    }

    private suspend fun invokeRemote(
        actionName: String,
        action: IControlPanelService.() -> Unit,
    ) {
        withContext(Dispatchers.IO) {
            val service = controlService ?: run {
                Log.w(TAG, "invokeRemote($actionName) ignored because service is null")
                return@withContext
            }
            try {
                Log.d(TAG, "invokeRemote($actionName)")
                service.action()
                refreshState(service)
            } catch (exception: RemoteException) {
                Log.e(TAG, "Remote AIDL call failed for $actionName", exception)
            }
        }
    }

    private fun refreshState(service: IControlPanelService) {
        val newState = mutableState.value.copy(
            rating = service.safeRating(),
            temperature = service.safeTemperature(),
            toggledOn = service.safeToggleState(),
            isServiceConnected = true,
        )
        Log.d(TAG, "refreshState() -> $newState")
        mutableState.update {
            it.copy(
                rating = newState.rating,
                temperature = newState.temperature,
                toggledOn = newState.toggledOn,
                isServiceConnected = newState.isServiceConnected,
            )
        }
    }

    private fun IControlPanelService.safeRating(): Int = try {
        getRating().coerceIn(0, MAX_RATING)
    } catch (exception: RemoteException) {
        Log.e(TAG, "Unable to read rating from service", exception)
        mutableState.value.rating
    }

    private fun IControlPanelService.safeTemperature(): Int = try {
        getTemperature()
    } catch (exception: RemoteException) {
        Log.e(TAG, "Unable to read temperature from service", exception)
        mutableState.value.temperature
    }

    private fun IControlPanelService.safeToggleState(): Boolean = try {
        isToggledOn()
    } catch (exception: RemoteException) {
        Log.e(TAG, "Unable to read toggle state from service", exception)
        mutableState.value.toggledOn
    }

    private companion object {
        private const val MAX_RATING = 5
        private const val TAG = "ControlPanelRepo"
    }
}
