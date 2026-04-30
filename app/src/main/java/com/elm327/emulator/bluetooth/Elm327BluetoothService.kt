package com.elm327.emulator.bluetooth

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
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
        private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        private const val CHANNEL_ID = "elm327_service_channel"
        private const val NOTIFICATION_ID = 1
    }

    private val binder = LocalBinder()
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isRunning = false
    private var isConnected = false

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
        stopServer()
        serviceScope.cancel()
        super.onDestroy()
    }

    fun startServer() {
        if (isRunning) return
        isRunning = true
        startForeground(NOTIFICATION_ID, createNotification())
        onLogMessage?.invoke("Starting ELM327 server...")
        serviceScope.launch { acceptConnections() }
    }

    fun stopServer() {
        isRunning = false
        isConnected = false
        onConnectionStateChanged?.invoke(false)
        closeConnection()
        serverSocket?.close()
        serverSocket = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        onLogMessage?.invoke("Server stopped")
    }

    private suspend fun acceptConnections() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            onLogMessage?.invoke("Bluetooth not available")
            stopServer()
            return
        }

        try {
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                "ELM327",
                UUID.fromString(SPP_UUID)
            )
            onLogMessage?.invoke("Server listening on RFCOMM...")

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
                    }
                }
            }
        } catch (e: IOException) {
            onLogMessage?.invoke("Server error: ${e.message}")
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
            val deviceName = clientSocket?.remoteDevice?.name ?: "Unknown"
            onLogMessage?.invoke("Connected to: $deviceName")

            val buffer = ByteArray(1024)
            var bytesRead: Int

            while (isRunning && isConnected) {
                try {
                    withContext(Dispatchers.IO) {
                        bytesRead = inputStream?.read(buffer) ?: 0
                    }
                    if (bytesRead > 0) {
                        val command = String(buffer, 0, bytesRead)
                        processCommand(command)
                    }
                } catch (e: IOException) {
                    if (isRunning) {
                        onLogMessage?.invoke("Read error: ${e.message}")
                    }
                    break
                }
            }
        } catch (e: IOException) {
            onLogMessage?.invoke("Connection error: ${e.message}")
        } finally {
            closeConnection()
            isConnected = false
            onConnectionStateChanged?.invoke(false)
            onLogMessage?.invoke("Client disconnected")
        }
    }

    private suspend fun processCommand(command: String) {
        if (parser.isEchoEnabled()) {
            send(command.trim())
        }

        val responses = parser.parse(command)
        for (response in responses) {
            send(response)
            onLogMessage?.invoke("RX: ${response.replace("\r", "<CR>")}")
        }
        send("\r") // Prompt
    }

    private suspend fun send(data: String) {
        try {
            outputStream?.write(data.toByteArray())
            outputStream?.flush()
            onLogMessage?.invoke("TX: ${data.replace("\r", "<CR>")}")
        } catch (e: IOException) {
            onLogMessage?.invoke("Write error: ${e.message}")
        }
    }

    private fun closeConnection() {
        try {
            inputStream?.close()
            outputStream?.close()
            clientSocket?.close()
        } catch (e: IOException) { }
        inputStream = null
        outputStream = null
        clientSocket = null
    }

    fun getLocalBluetoothAddress(): String {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter?.address ?: "00:00:00:00:00:00"
    }

    fun isServerRunning() = isRunning
    fun isClientConnected() = isConnected

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
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_sort_by_size)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
