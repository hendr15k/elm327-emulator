package com.elm327.emulator

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.elm327.emulator.bluetooth.Elm327BluetoothService
import com.elm327.emulator.databinding.ActivityMainBinding
import com.elm327.emulator.ui.LogAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var logAdapter: LogAdapter

    private var bluetoothService: Elm327BluetoothService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as Elm327BluetoothService.LocalBinder
            bluetoothService = binder.getService()
            serviceBound = true
            setupServiceCallbacks()
            updateUI()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothService = null
            serviceBound = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            startBluetoothService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkAndRequestPermissions()
    }

    override fun onStart() {
        super.onStart()
        Intent(this, Elm327BluetoothService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            bluetoothService?.onConnectionStateChanged = null
            bluetoothService?.onLogMessage = null
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun setupUI() {
        logAdapter = LogAdapter()
        binding.logRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = logAdapter
        }

        binding.startStopButton.setOnClickListener {
            if (bluetoothService?.isServerRunning() == true) {
                stopBluetoothService()
            } else {
                if (hasRequiredPermissions()) {
                    startBluetoothService()
                } else {
                    checkAndRequestPermissions()
                }
            }
        }

        binding.clearLogButton.setOnClickListener {
            logAdapter.clear()
        }
    }

    private fun setupServiceCallbacks() {
        bluetoothService?.apply {
            onConnectionStateChanged = { connected ->
                runOnUiThread { updateConnectionStatus(connected) }
            }
            onLogMessage = { message ->
                runOnUiThread { logAdapter.addMessage(message) }
            }
        }
    }

    private fun updateUI() {
        bluetoothService?.let { service ->
            if (hasRequiredPermissions()) {
                binding.btAddressText.text = service.getLocalBluetoothAddress().uppercase()
            } else {
                binding.btAddressText.text = "N/A"
            }
            updateConnectionStatus(service.isClientConnected())
            binding.startStopButton.text = if (service.isServerRunning()) {
                getString(R.string.stop_server)
            } else {
                getString(R.string.start_server)
            }
        }
    }

    private fun updateConnectionStatus(connected: Boolean) {
        val (statusText, statusColor) = when {
            connected -> Pair(getString(R.string.status_connected), R.color.status_green)
            bluetoothService?.isServerRunning() == true -> Pair(getString(R.string.status_listening), R.color.status_orange)
            else -> Pair(getString(R.string.status_disconnected), R.color.status_red)
        }
        binding.statusText.text = statusText
        binding.statusIndicator.setBackgroundColor(ContextCompat.getColor(this, statusColor))

        binding.startStopButton.text = if (bluetoothService?.isServerRunning() == true) {
            getString(R.string.stop_server)
        } else {
            getString(R.string.start_server)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            updateUI()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun startBluetoothService() {
        val intent = Intent(this, Elm327BluetoothService::class.java).apply {
            action = "START"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopBluetoothService() {
        bluetoothService?.stopServer()
    }
}
