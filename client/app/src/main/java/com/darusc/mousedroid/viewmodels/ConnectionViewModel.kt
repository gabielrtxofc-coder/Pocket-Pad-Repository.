package com.darusc.mousedroid.viewmodels

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.viewModelScope
import com.darusc.mousedroid.getDeviceDetails
import com.darusc.mousedroid.networking.Connection
import com.darusc.mousedroid.networking.ConnectionManager
import com.darusc.mousedroid.networking.bluetooth.BluetoothAdapterWrapper
import com.darusc.mousedroid.networking.hasUsbConnection
import com.darusc.mousedroid.networking.hasWifiConnection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConnectionViewModel :
    BaseViewModel<ConnectionViewModel.State, ConnectionViewModel.Event>(State.Idle),
    ConnectionManager.ConnectionStateCallback {

    sealed class State : BaseViewModel.State() {
        object Idle : State()
        data class Searching(val message: String) : State()
        data class Connecting(val message: String) : State()
        data class Connected(val connectionMode: Connection.Mode, val hostName: String) : State()
        data class Error(val message: String, val suggestion: String) : State()
        data class Reconnecting(val attempt: Int) : State()
    }

    sealed class Event : BaseViewModel.Event() {
        data class Navigate(@IdRes val id: Int) : Event()
        object NavigateToInput : Event()
        object NavigateToMain : Event()
        data class NavigateToDeviceList(val mode: Connection.Mode) : Event()

        object EnableBluetooth : Event()

        data class ConnectionFailed(val connectionMode: Connection.Mode) : Event()
        data class ConnectionDisconnected(val connectionMode: Connection.Mode, val hostName: String) : Event()
    }

    private val connectionManager = ConnectionManager.getInstance(this)

    private var autoConnectJob: Job? = null
    private var reconnectJob: Job? = null
    private var autoConnectAttempts = 0
    private val maxAutoConnectAttempts = 5
    private var isAutoConnecting = false

    companion object {
        private const val RECONNECT_BASE_DELAY_MS = 1000L
        private const val RECONNECT_MAX_DELAY_MS = 10000L
        private const val AUTO_CONNECT_DELAY_MS = 500L
    }

    override fun onConnectionInitiated(mode: Connection.Mode) {
        val currentState = state.value
        if (currentState is State.Idle || currentState is State.Searching || currentState is State.Reconnecting) {
            setState(State.Connecting(
                when (mode) {
                    Connection.Mode.USB -> "Connecting via USB..."
                    Connection.Mode.WIFI -> "Connecting via WiFi..."
                    Connection.Mode.BLUETOOTH -> "Connecting via Bluetooth..."
                }
            ))
        }
    }

    override fun onConnectionSuccessful(connectionMode: Connection.Mode, hostName: String) {
        autoConnectAttempts = 0
        isAutoConnecting = false
        setState(State.Connected(connectionMode, hostName))
        sendEvent(Event.NavigateToInput)
    }

    override fun onConnectionFailed(connectionMode: Connection.Mode) {
        if (isAutoConnecting) {
            // Part of auto-connect loop, don't show error yet
            return
        }
        
        val reconnectAttempt = reconnectJob?.isActive == true
        if (reconnectAttempt) {
            // Part of reconnection loop, update state silently
            return
        }
        
        setState(State.Idle)
        sendEvent(Event.ConnectionFailed(connectionMode))
    }

    override fun onDisconnected(connectionMode: Connection.Mode, hostName: String) {
        setState(State.Idle)
        sendEvent(Event.ConnectionDisconnected(connectionMode, hostName))
        
        // Auto-reconnect for USB connections
        if (connectionMode == Connection.Mode.USB) {
            startReconnection()
        }
    }

    /**
     * Start automatic connection when USB is detected.
     * This is called when the app starts or when returning to the main screen.
     */
    fun tryAutoConnect(context: Context) {
        if (state.value is State.Connected || state.value is State.Connecting || isAutoConnecting) {
            return // Already connected or connecting
        }
        
        autoConnectJob?.cancel()
        autoConnectJob = viewModelScope.launch {
            isAutoConnecting = true
            autoConnectAttempts = 0
            
            while (autoConnectAttempts < maxAutoConnectAttempts && isAutoConnecting) {
                autoConnectAttempts++
                
                if (hasUsbConnection(context)) {
                    setState(State.Searching("Connecting to computer..."))
                    connectionManager.connectUSB(6969, getDeviceDetails(context, Connection.Mode.USB))
                    
                    // Wait for connection attempt to complete
                    delay(3000)
                    
                    if (state.value is State.Connected) {
                        return@launch
                    }
                }
                
                if (autoConnectAttempts < maxAutoConnectAttempts) {
                    delay(AUTO_CONNECT_DELAY_MS)
                }
            }
            
            isAutoConnecting = false
            
            // If we couldn't auto-connect, show idle state
            if (state.value !is State.Connected) {
                setState(State.Idle)
            }
        }
    }

    /**
     * Start reconnection loop after disconnection.
     */
    private fun startReconnection() {
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            var attempt = 0
            
            while (attempt < 10) {
                attempt++
                
                val delayMs = minOf(
                    RECONNECT_BASE_DELAY_MS * (1L shl minOf(attempt - 1, 4)),
                    RECONNECT_MAX_DELAY_MS
                )
                
                setState(State.Reconnecting(attempt))
                delay(delayMs)
                
                // Check if we should stop (user navigated away or connected differently)
                val currentState = state.value
                if (currentState is State.Connected || currentState is State.Idle && !isAutoConnecting) {
                    break
                }
                
                // Check if USB is still available
                // Note: We can't access context here directly, so we just try to connect
                // The ConnectionManager will fail if USB is not available
            }
        }
    }

    /**
     * Start server mode with autodetect.
     * USB is auto-detected. If available, connect directly.
     * Otherwise, navigate to device list for WiFi.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun startServerMode(context: Context) {
        if (hasUsbConnection(context)) {
            // USB detected - connect directly without navigating to device list
            autoConnectJob?.cancel()
            isAutoConnecting = true
            connectionManager.connectUSB(6969, getDeviceDetails(context, Connection.Mode.USB))
        } else if (hasWifiConnection(context)) {
            // No USB - navigate to WiFi device list
            sendEvent(Event.NavigateToDeviceList(Connection.Mode.WIFI))
        }
    }

    /**
     * Start bluetooth mode.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun startBluetoothMode(context: Context, afterEnableIntent: Boolean = false) {
        if (afterEnableIntent || BluetoothAdapterWrapper.getInstance()?.isEnabled!!) {
            connectionManager.registerBluetoothHID(context)
            sendEvent(Event.NavigateToDeviceList(Connection.Mode.BLUETOOTH))
        } else {
            sendEvent(Event.EnableBluetooth)
        }
    }

    /**
     * Manual disconnect requested by user.
     */
    fun disconnect() {
        autoConnectJob?.cancel()
        reconnectJob?.cancel()
        isAutoConnecting = false
        autoConnectAttempts = 0
        
        viewModelScope.launch {
            connectionManager.disconnect()
            setState(State.Idle)
            sendEvent(Event.NavigateToMain)
        }
    }

    /**
     * Cancel any ongoing auto-connect/reconnect and reset state.
     */
    fun cancelAutoConnect() {
        autoConnectJob?.cancel()
        reconnectJob?.cancel()
        isAutoConnecting = false
        autoConnectAttempts = 0
    }

    override fun onCleared() {
        super.onCleared()
        cancelAutoConnect()
    }
}
