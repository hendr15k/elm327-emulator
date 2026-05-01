package com.elm327.emulator.bluetooth

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.elm327.emulator.MainActivity
import com.elm327.emulator.R
import com.elm327.emulator.data.Elm327Parser
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class Elm327BluetoothService : Service() {

    companion object {
        private const val TAG = "ELM327Service"
        private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        private const val CHANNEL_ID = "elm327_service_channel"
        private const val NOTIFICATION_ID = 1
        const val DEVICE_NAME = "ELM327"
        private val ADAPTER_NAME = "ELM327"
    }

    private val binder = LocalBinder()
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isRunning = false
    private var isConnected = false
    private var pairingReceiver: PairingReceiver? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val parser = Elm327Parser()

    var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    var onLogMessage: ((String) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): Elm327BluetoothService = this@Elm327BluetoothService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> startServer()
            "STOP" -> stopServer()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterPairingReceiver()
        stopServer()
        serviceScope.cancel()
        super.onDestroy()
    }

    fun startServer() {
        if (isRunning) return
        isRunning = true
        startForeground(NOTIFICATION_ID, createNotification())
        registerPairingReceiver()
        setAdapterName()
        onLogMessage?.invoke("Starting ELM327 server...")
        onConnectionStateChanged?.invoke(false)
        serviceScope.launch { acceptConnections() }
    }

    fun stopServer() {
        isRunning = false
        isConnected = false
        onConnectionStateChanged?.invoke(false)
        closeConnection()
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server socket: ${e.message}")
        }
        serverSocket = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        onLogMessage?.invoke("Server stopped")
    }

    private fun registerPairingReceiver() {
        if (pairingReceiver != null) return
        try {
            pairingReceiver = PairingReceiver()
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(pairingReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(pairingReceiver, filter)
            }
            Log.d(TAG, "Pairing receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register pairing receiver: ${e.message}")
        }
    }

    private fun unregisterPairingReceiver() {
        try {
            pairingReceiver?.let {
                unregisterReceiver(it)
                pairingReceiver = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister pairing receiver: ${e.message}")
        }
    }

    private fun setAdapterName() {
        try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter ?: return
            if (adapter.isEnabled && hasBluetoothConnectPermission()) {
                adapter.name = ADAPTER_NAME
                Log.d(TAG, "Adapter name set to: $ADAPTER_NAME")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "No permission to set adapter name: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set adapter name: ${e.message}")
        }
    }

    private suspend fun acceptConnections() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            onLogMessage?.invoke("Bluetooth not available or disabled")
            stopServer()
            return
        }

        try {
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                DEVICE_NAME,
                UUID.fromString(SPP_UUID)
            )
            onLogMessage?.invoke("Server listening on RFCOMM...")
            Log.d(TAG, "Server listening on RFCOMM, name=$DEVICE_NAME, uuid=$SPP_UUID")

            while (isRunning) {
                try {
                    clientSocket = withContext(Dispatchers.IO) {
                        serverSocket?.accept()
                    }
                    if (clientSocket != null) {
                        handleConnection()
                    }
                } catch (e: IOException) {
                    if (isRunning) {
                        onLogMessage?.invoke("Accept error: ${e.message}")
                        Log.e(TAG, "Accept error: ${e.message}")
                    }
                }
            }
        } catch (e: SecurityException) {
            onLogMessage?.invoke("Bluetooth permission denied: ${e.message}")
            Log.e(TAG, "Bluetooth permission denied: ${e.message}")
            stopServer()
        } catch (e: IOException) {
            onLogMessage?.invoke("Server error: ${e.message}")
            Log.e(TAG, "Server error: ${e.message}")
            stopServer()
        }
    }

    private suspend fun handleConnection() {
        isConnected = true
        onConnectionStateChanged?.invoke(true)
        parser.reset()

        try {
            inputStream = clientSocket?.inputStream
            outputStream = clientSocket?.outputStream
            val deviceName = clientSocket?.remoteDevice?.let {
                try { it.name } catch (e: SecurityException) { "Unknown" }
            } ?: "Unknown"
            val deviceAddress = clientSocket?.remoteDevice?.let {
                try { it.address } catch (e: SecurityException) { "Unknown" }
            } ?: "Unknown"
            onLogMessage?.invoke("Connected to: $deviceName ($deviceAddress)")
            Log.d(TAG, "Client connected: $deviceName ($deviceAddress)")

            sendInitPrompt()

            val buffer = ByteArray(1024)
            var bytesRead: Int

            while (isRunning && isConnected) {
                try {
                    withContext(Dispatchers.IO) {
                        bytesRead = inputStream?.read(buffer) ?: 0
                    }
                    if (bytesRead > 0) {
                        val raw = String(buffer, 0, bytesRead)
                        processCommand(raw)
                    } else if (bytesRead == -1) {
                        break
                    }
                } catch (e: IOException) {
                    if (isRunning) {
                        onLogMessage?.invoke("Read error: ${e.message}")
                        Log.e(TAG, "Read error: ${e.message}")
                    }
                    break
                }
            }
        } catch (e: IOException) {
            onLogMessage?.invoke("Connection error: ${e.message}")
            Log.e(TAG, "Connection error: ${e.message}")
        } finally {
            closeConnection()
            isConnected = false
            onConnectionStateChanged?.invoke(false)
            onLogMessage?.invoke("Client disconnected")
        }
    }

    private suspend fun sendInitPrompt() {
        withContext(Dispatchers.IO) {
            try {
                val prompt = byteArrayOf(0x3E, 0x20, 0x3E)
                outputStream?.write(prompt)
                outputStream?.flush()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to send init prompt: ${e.message}")
            }
        }
    }

    private suspend fun processCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            sendPrompt()
            return
        }

        Log.d(TAG, "RECV: $trimmed")

        if (parser.isEchoEnabled()) {
            sendRaw(trimmed)
        }

        val responses = parser.parse(trimmed)
        for (response in responses) {
            sendRaw(response)
            onLogMessage?.invoke("RX: $response")
        }
        sendPrompt()
    }

    private fun sendPrompt() {
        try {
            outputStream?.write("\r>".toByteArray())
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to send prompt: ${e.message}")
        }
    }

    private fun sendRaw(data: String) {
        try {
            outputStream?.write(data.toByteArray())
            outputStream?.flush()
            Log.d(TAG, "SEND: $data")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to send: ${e.message}")
        }
    }

    private fun closeConnection() {
        try {
            inputStream?.close()
        } catch (e: Exception) { }
        try {
            outputStream?.close()
        } catch (e: Exception) { }
        try {
            clientSocket?.close()
        } catch (e: Exception) { }
        inputStream = null
        outputStream = null
        clientSocket = null
    }

    fun getLocalBluetoothAddress(): String {
        return try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val address = bluetoothManager.adapter?.address
            if (address == "02:00:00:00:00:00") "N/A (Emulator)" else address ?: "N/A"
        } catch (e: SecurityException) {
            "N/A"
        }
    }

    fun isServerRunning() = isRunning
    fun isClientConnected() = isConnected

    private fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ELM327 Emulator")
            .setContentText("Bluetooth server is running")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
