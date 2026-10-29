package com.elm327.emulator.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.os.Build

class PairingReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PairingReceiver"
        private const val PAIRING_VARIANT_PIN = 0
        private const val PAIRING_VARIANT_PASSKEY = 1
        private const val PAIRING_VARIANT_PASSKEY_CONFIRMATION = 2
        private const val PAIRING_VARIANT_CONSENT = 3
        private const val PAIRING_VARIANT_PASSKEY_DISPLAY = 4
        private const val PAIRING_VARIANT_OOB_CONSENT = 5

        private val PIN_0000 = byteArrayOf(0x30, 0x30, 0x30, 0x30)
        private val PASSKEY_000000 = byteArrayOf(0x30, 0x30, 0x30, 0x30, 0x30, 0x30)
    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                val variant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, -1)
                Log.d(TAG, "Pairing request from: ${device?.name} (${device?.address}), variant: $variant")

                try {
                    when (variant) {
                        PAIRING_VARIANT_PIN -> {
                            // SPP adapters pair with the classic 4-digit PIN.
                            device?.setPin(PIN_0000)
                            Log.d(TAG, "PIN set to 0000")
                        }
                        PAIRING_VARIANT_PASSKEY -> {
                            // A passkey is a 6-digit value; a 4-byte PIN is not
                            // a valid passkey and was rejected by the stack.
                            device?.setPin(PASSKEY_000000)
                            Log.d(TAG, "Passkey set to 000000")
                        }
                        PAIRING_VARIANT_PASSKEY_CONFIRMATION, PAIRING_VARIANT_CONSENT -> {
                            device?.setPairingConfirmation(true)
                            Log.d(TAG, "Pairing confirmation accepted")
                        }
                        PAIRING_VARIANT_PASSKEY_DISPLAY -> {
                            device?.setPairingConfirmation(true)
                            Log.d(TAG, "Passkey display - auto confirmed")
                        }
                        PAIRING_VARIANT_OOB_CONSENT -> {
                            device?.setPairingConfirmation(true)
                            Log.d(TAG, "OOB consent accepted")
                        }
                        else -> {
                            device?.setPairingConfirmation(true)
                            Log.d(TAG, "Generic pairing accepted")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to handle pairing: ${e.message}")
                }
            }
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                val stateStr = when (bondState) {
                    BluetoothDevice.BOND_NONE -> "NONE"
                    BluetoothDevice.BOND_BONDING -> "BONDING"
                    BluetoothDevice.BOND_BONDED -> "BONDED"
                    else -> "UNKNOWN"
                }
                Log.d(TAG, "Bond state: ${device?.address} -> $stateStr")
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                Log.d(TAG, "ACL Connected: ${device?.name}")
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                Log.d(TAG, "ACL Disconnected: ${device?.name}")
            }
        }
    }
}
