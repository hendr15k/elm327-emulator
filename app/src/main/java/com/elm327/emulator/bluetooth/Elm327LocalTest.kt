package com.elm327.emulator.bluetooth

import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.elm327.emulator.data.Elm327Parser

class Elm327LocalTest {

    companion object {
        private const val TAG = "ELM327LocalTest"
        private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }

    data class TestResult(val name: String, val passed: Boolean, val detail: String)

    fun runAllTests(context: Context): List<TestResult> {
        val results = mutableListOf<TestResult>()
        val r1 = testAdapterName(context)
        Log.d(TAG, "${r1.name}: ${if (r1.passed) "PASS" else "FAIL"} - ${r1.detail}")
        results.add(r1)
        val r2 = testAdapterEnabled(context)
        Log.d(TAG, "${r2.name}: ${if (r2.passed) "PASS" else "FAIL"} - ${r2.detail}")
        results.add(r2)
        val r3 = testAdapterAddress(context)
        Log.d(TAG, "${r3.name}: ${if (r3.passed) "PASS" else "FAIL"} - ${r3.detail}")
        results.add(r3)
        val r4 = testParserATCommands()
        Log.d(TAG, "${r4.name}: ${if (r4.passed) "PASS" else "FAIL"} - ${r4.detail}")
        results.add(r4)
        val r5 = testParserOBDMode01()
        Log.d(TAG, "${r5.name}: ${if (r5.passed) "PASS" else "FAIL"} - ${r5.detail}")
        results.add(r5)
        val r6 = testParserOBDMode03()
        Log.d(TAG, "${r6.name}: ${if (r6.passed) "PASS" else "FAIL"} - ${r6.detail}")
        results.add(r6)
        val r7 = testParserOBDMode09()
        Log.d(TAG, "${r7.name}: ${if (r7.passed) "PASS" else "FAIL"} - ${r7.detail}")
        results.add(r7)
        val r8 = testParserReset()
        Log.d(TAG, "${r8.name}: ${if (r8.passed) "PASS" else "FAIL"} - ${r8.detail}")
        results.add(r8)
        val r9 = testParserEcho()
        Log.d(TAG, "${r9.name}: ${if (r9.passed) "PASS" else "FAIL"} - ${r9.detail}")
        results.add(r9)
        val r10 = testParserHeaders()
        Log.d(TAG, "${r10.name}: ${if (r10.passed) "PASS" else "FAIL"} - ${r10.detail}")
        results.add(r10)
        val r11 = testParserProtocol()
        Log.d(TAG, "${r11.name}: ${if (r11.passed) "PASS" else "FAIL"} - ${r11.detail}")
        results.add(r11)
        val r12 = testRFCOMMRegistration(context)
        Log.d(TAG, "${r12.name}: ${if (r12.passed) "PASS" else "FAIL"} - ${r12.detail}")
        results.add(r12)
        val passed = results.count { it.passed }
        Log.d(TAG, "=== RESULTS: $passed/${results.size} PASSED ===")
        return results
    }

    private fun testAdapterName(context: Context): TestResult {
        return try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bm.adapter
            val name = adapter?.name ?: "null"
            TestResult("Adapter Name", name == "ELM327", "Name='$name'")
        } catch (e: Exception) {
            TestResult("Adapter Name", false, e.message ?: "unknown")
        }
    }

    private fun testAdapterEnabled(context: Context): TestResult {
        return try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val enabled = bm.adapter?.isEnabled == true
            TestResult("Adapter Enabled", enabled, "enabled=$enabled")
        } catch (e: Exception) {
            TestResult("Adapter Enabled", false, e.message ?: "unknown")
        }
    }

    private fun testAdapterAddress(context: Context): TestResult {
        return try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val address = bm.adapter?.address ?: "null"
            val valid = address != "02:00:00:00:00:00" && address.matches(Regex("[0-9A-F]{2}(:[0-9A-F]{2}){5}"))
            TestResult("Adapter Address", valid, "address=$address")
        } catch (e: Exception) {
            TestResult("Adapter Address", false, e.message ?: "unknown")
        }
    }

    private fun testParserATCommands(): TestResult {
        val parser = Elm327Parser()
        parser.reset()

        val ati = parser.parse("ATI")
        val atz = parser.parse("ATZ")
        val atsp = parser.parse("ATSP5")
        val atdp = parser.parse("ATDP")
        val atrv = parser.parse("ATRV")

        val allOk = ati[0] == "ELM327 v1.5" &&
            atz[0] == "ELM327 v1.5" &&
            atsp[0] == "OK" &&
            atdp[0] == "ISO 15765-4 (CAN)" &&
            atrv[0].contains("V")

        return TestResult("AT Commands", allOk, "ATI=$ati, ATZ=$atz, ATSP=$atsp, ATDP=$atdp, ATRV=$atrv")
    }

    private fun testParserOBDMode01(): TestResult {
        val parser = Elm327Parser()
        parser.reset()

        val rpm = parser.parse("010C")
        val speed = parser.parse("010D")
        val temp = parser.parse("0105")
        val load = parser.parse("0104")
        val fuel = parser.parse("012F")

        val allOk = rpm[0].startsWith("410C") &&
            speed[0].startsWith("410D") &&
            temp[0].startsWith("4105") &&
            load[0].startsWith("4104") &&
            fuel[0].startsWith("412F")

        return TestResult("OBD Mode 01 PIDs", allOk, "RPM=$rpm, SPD=$speed, TEMP=$temp, LOAD=$load, FUEL=$fuel")
    }

    private fun testParserOBDMode03(): TestResult {
        val parser = Elm327Parser()
        parser.reset()

        val dtc = parser.parse("03")
        val ok = dtc[0].startsWith("43") && dtc[0].contains("00 00 00 00")
        return TestResult("OBD Mode 03 DTCs", ok, "Mode03=$dtc")
    }

    private fun testParserOBDMode09(): TestResult {
        val parser = Elm327Parser()
        parser.reset()

        val vin = parser.parse("0904")
        val info = parser.parse("09")
        val ok = vin[0].startsWith("49 04") && info[0].startsWith("49 00")
        return TestResult("OBD Mode 09 Vehicle Info", ok, "VIN=$vin, INFO=$info")
    }

    private fun testParserReset(): TestResult {
        val parser = Elm327Parser()
        parser.parse("ATH1")
        parser.parse("ATE0")
        parser.parse("ATL1")

        parser.reset()

        val echoOn = parser.isEchoEnabled()
        val headersOff = !parser.isHeadersEnabled()
        val linefeedOff = !parser.isLineFeedEnabled()

        val ok = echoOn && headersOff && linefeedOff
        return TestResult("Parser Reset", ok, "echo=$echoOn, headers=${parser.isHeadersEnabled()}, lf=${parser.isLineFeedEnabled()}")
    }

    private fun testParserEcho(): TestResult {
        val parser = Elm327Parser()
        parser.reset()
        parser.parse("ATE0")
        val off = !parser.isEchoEnabled()
        parser.parse("ATE1")
        val on = parser.isEchoEnabled()
        return TestResult("Echo Toggle", off && on, "off=$off, on=$on")
    }

    private fun testParserHeaders(): TestResult {
        val parser = Elm327Parser()
        parser.reset()
        parser.parse("ATH1")
        val on = parser.isHeadersEnabled()
        parser.parse("ATH0")
        val off = !parser.isHeadersEnabled()

        parser.parse("ATH0")
        val noHeaders = parser.parse("010C")
        parser.parse("ATH1")
        val withHeaders = parser.parse("010C")

        val ok = on && off && withHeaders[0].contains("41 0C") && noHeaders[0].startsWith("410C")
        return TestResult("Headers Toggle", ok, "withH=$withHeaders, noH=$noHeaders")
    }

    private fun testParserProtocol(): TestResult {
        val parser = Elm327Parser()
        parser.reset()

        val protocols = mapOf(
            "ATSP0" to "AUTO",
            "ATSP1" to "SAE J1850 PWM",
            "ATSP2" to "SAE J1850 VPW",
            "ATSP3" to "ISO 9141-2",
            "ATSP4" to "ISO 14230-4 (KWP2000)",
            "ATSP5" to "ISO 15765-4 (CAN)"
        )

        var allOk = true
        val details = StringBuilder()
        for ((cmd, expected) in protocols) {
            parser.parse(cmd)
            val dp = parser.parse("ATDP")[0]
            val match = dp == expected
            if (!match) allOk = false
            details.append("$cmd -> $dp (expected $expected) ")
        }
        return TestResult("Protocol Selection", allOk, details.toString())
    }

    private fun testRFCOMMRegistration(context: Context): TestResult {
        return try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bm.adapter
            if (adapter == null || !adapter.isEnabled) {
                return TestResult("RFCOMM Registration", false, "BT not available")
            }
            val name = try { adapter.name } catch (e: SecurityException) { "N/A" }
            val address = try { adapter.address } catch (e: SecurityException) { "N/A" }
            val scanMode = try { adapter.scanMode } catch (e: SecurityException) { -1 }

            val nameOk = name == "ELM327"
            val addressOk = address != "02:00:00:00:00:00"
            val allOk = nameOk && addressOk

            val details = "name=$name addr=$address scan=$scanMode"
            TestResult("RFCOMM Registration", allOk, details)
        } catch (e: Exception) {
            TestResult("RFCOMM Registration", false, "Error: ${e.message}")
        }
    }
}
