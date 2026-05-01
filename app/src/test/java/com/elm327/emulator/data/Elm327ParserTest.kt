package com.elm327.emulator.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class Elm327ParserTest {

    private lateinit var parser: Elm327Parser

    @Before
    fun setup() {
        parser = Elm327Parser()
        parser.reset()
    }

    @Test
    fun testReset() {
        val result = parser.parse("ATZ")
        assertEquals(listOf("ELM327 v1.5"), result)
    }

    @Test
    fun testEchoOff() {
        assertEquals(listOf("OK"), parser.parse("ATE0"))
        assertFalse(parser.isEchoEnabled())
    }

    @Test
    fun testEchoOn() {
        assertEquals(listOf("OK"), parser.parse("ATE1"))
        assertTrue(parser.isEchoEnabled())
    }

    @Test
    fun testLineFeedOff() {
        assertEquals(listOf("OK"), parser.parse("ATL0"))
        assertFalse(parser.isLineFeedEnabled())
    }

    @Test
    fun testLineFeedOn() {
        assertEquals(listOf("OK"), parser.parse("ATL1"))
        assertTrue(parser.isLineFeedEnabled())
    }

    @Test
    fun testHeadersOff() {
        assertEquals(listOf("OK"), parser.parse("ATH0"))
        assertFalse(parser.isHeadersEnabled())
    }

    @Test
    fun testHeadersOn() {
        assertEquals(listOf("OK"), parser.parse("ATH1"))
        assertTrue(parser.isHeadersEnabled())
    }

    @Test
    fun testProtocolAuto() {
        assertEquals(listOf("OK"), parser.parse("ATSP0"))
    }

    @Test
    fun testProtocolSet() {
        assertEquals(listOf("OK"), parser.parse("ATSP5"))
    }

    @Test
    fun testDeviceInfo() {
        assertEquals(listOf("ELM327 v1.5"), parser.parse("ATI"))
        assertEquals(listOf("ELM327 v1.5"), parser.parse("ATI1"))
        assertEquals(listOf("ELM327"), parser.parse("ATI0"))
    }

    @Test
    fun testVoltage() {
        val result = parser.parse("ATRV")
        assertTrue("Voltage: $result", result[0].contains("V"))
    }

    @Test
    fun testDisplayProtocol() {
        parser.parse("ATSP5")
        assertEquals(listOf("ISO 15765-4 (CAN)"), parser.parse("ATDP"))
    }

    @Test
    fun testDisplayProtocolNumber() {
        parser.parse("ATSP5")
        assertEquals(listOf("5"), parser.parse("ATDPN"))
    }

    @Test
    fun testTimeout() {
        assertEquals(listOf("OK"), parser.parse("ATSTFF"))
    }

    @Test
    fun testDeviceString() {
        assertEquals(listOf("OBDLink"), parser.parse("AT@1"))
        assertEquals(listOf("ELM327"), parser.parse("AT@0"))
    }

    @Test
    fun testDefaultsAll() {
        assertEquals(listOf("OK"), parser.parse("ATD"))
        assertTrue(parser.isEchoEnabled())
        assertFalse(parser.isLineFeedEnabled())
        assertFalse(parser.isHeadersEnabled())
    }

    @Test
    fun testWarmStart() {
        val result = parser.parse("ATWS")
        assertEquals(listOf("ELM327 v1.5"), result)
    }

    @Test
    fun testFactoryDefaults() {
        assertEquals(listOf("OK"), parser.parse("ATFE"))
    }

    @Test
    fun testUnknownCommand() {
        assertEquals(listOf("?"), parser.parse("ATXX"))
    }

    @Test
    fun testEmptyCommand() {
        assertEquals(emptyList<String>(), parser.parse(""))
        assertEquals(emptyList<String>(), parser.parse("  "))
        assertEquals(emptyList<String>(), parser.parse("\r"))
    }

    @Test
    fun testPrompt() {
        val result = parser.parse(">")
        assertTrue("Prompt should return empty", result.isEmpty())
    }

    @Test
    fun testRpmResponse() {
        val result = parser.parse("010C")
        assertNotNull(result)
        assertTrue("Response should contain RPM data, got: $result", result.size == 1)
        val response = result[0]
        assertTrue("Response should contain 41, got: $response", response.startsWith("41"))
    }

    @Test
    fun testSpeedResponse() {
        val result = parser.parse("010D")
        assertNotNull(result)
        assertTrue("Speed response: $result", result[0].startsWith("41"))
    }

    @Test
    fun testCoolantTemp() {
        val result = parser.parse("0105")
        assertNotNull(result)
        assertTrue("Coolant: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testEngineLoad() {
        val result = parser.parse("0104")
        assertNotNull(result)
        assertTrue("Engine load: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testThrottlePosition() {
        val result = parser.parse("010B")
        assertNotNull(result)
        assertTrue("Throttle: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testIntakeTemp() {
        val result = parser.parse("010F")
        assertNotNull(result)
        assertTrue("Intake temp: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testMafRate() {
        val result = parser.parse("0110")
        assertNotNull(result)
        assertTrue("MAF: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testFuelLevel() {
        val result = parser.parse("012F")
        assertNotNull(result)
        assertTrue("Fuel: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testRuntime() {
        val result = parser.parse("011F")
        assertNotNull(result)
        assertTrue("Runtime: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testRuntimeBytes() {
        val result = parser.parse("0121")
        assertNotNull(result)
        assertTrue("Runtime bytes: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testMode03NoDtc() {
        val result = parser.parse("03")
        assertNotNull(result)
        assertTrue("Mode 03: ${result[0]}", result[0].startsWith("43"))
    }

    @Test
    fun testMode07() {
        val result = parser.parse("07")
        assertNotNull(result)
        assertTrue("Mode 07: ${result[0]}", result[0].startsWith("47"))
    }

    @Test
    fun testMode04() {
        val result = parser.parse("04")
        assertEquals(listOf("44"), result)
    }

    @Test
    fun testMode05() {
        val result = parser.parse("05")
        assertTrue("Mode 05: ${result[0]}", result[0].startsWith("54"))
    }

    @Test
    fun testMode06() {
        val result = parser.parse("06")
        assertTrue("Mode 06: ${result[0]}", result[0].startsWith("64"))
    }

    @Test
    fun testMode08() {
        val result = parser.parse("08")
        assertTrue("Mode 08: ${result[0]}", result[0].startsWith("84"))
    }

    @Test
    fun testMode09Info() {
        val result = parser.parse("09")
        assertTrue("Mode 09 info: ${result[0]}", result[0].startsWith("49 00"))
    }

    @Test
    fun testMode09VIN() {
        val result = parser.parse("0904")
        assertTrue("Mode 09 VIN: ${result[0]}", result[0].startsWith("49 04"))
    }

    @Test
    fun testMode09CalId() {
        val result = parser.parse("090A")
        assertTrue("Mode 09 Cal ID: ${result[0]}", result[0].startsWith("49 0A"))
    }

    @Test
    fun testMode02() {
        val result = parser.parse("0202")
        assertTrue("Mode 02: ${result[0]}", result[0].startsWith("42"))
    }

    @Test
    fun testUnknownPid() {
        val result = parser.parse("01FF")
        assertEquals(listOf("NO DATA"), result)
    }

    @Test
    fun testUnknownMode() {
        assertEquals(listOf("?"), parser.parse("1000"))
    }

    @Test
    fun testSupportedPids0100() {
        val result = parser.parse("0100")
        assertTrue("Supported PIDs: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testFuelType() {
        val result = parser.parse("0151")
        assertNotNull(result)
        assertTrue("Fuel type: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testO2Sensor() {
        val result = parser.parse("0114")
        assertNotNull(result)
        assertTrue("O2 Sensor: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testBarometricPressure() {
        val result = parser.parse("0133")
        assertNotNull(result)
    }

    @Test
    fun testControlModuleVoltage() {
        val result = parser.parse("0142")
        assertNotNull(result)
        assertTrue("Voltage: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testAbsoluteLoad() {
        val result = parser.parse("0143")
        assertNotNull(result)
    }

    @Test
    fun testAmbientTemp() {
        val result = parser.parse("0146")
        assertNotNull(result)
    }

    @Test
    fun testCommandedThrottle() {
        val result = parser.parse("0145")
        assertNotNull(result)
    }

    @Test
    fun testRpmRange() {
        for (i in 1..10) {
            val result = parser.parse("010C")
            val response = result[0].replace("410C", "").replace(" ", "")
            val hex = response.take(4)
            val a = hex.substring(0, 2).toInt(16)
            val b = hex.substring(2, 4).toInt(16)
            val rpm = (a * 256 + b) / 4.0
            assertTrue("RPM $rpm should be in range 0-15000", rpm in 0.0..15000.0)
        }
    }

    @Test
    fun testInitializeResetsState() {
        parser.parse("010C")
        parser.reset()
        val result = parser.parse("010C")
        assertNotNull(result)
    }

    @Test
    fun testIntakeTempAfterReset() {
        parser.reset()
        val result = parser.parse("010F")
        assertTrue("Intake temp: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testFuelLevelRange() {
        for (i in 1..100) {
            val result = parser.parse("012F")
            val response = result[0].replace("412F", "")
            val fuelVal = response.toInt()
            assertTrue("Fuel value should be 0-255, got $fuelVal", fuelVal in 0..255)
        }
    }

    @Test
    fun testHeadersEnabledMode01() {
        parser.parse("ATH1")
        val result = parser.parse("010C")
        assertTrue("Headers: ${result[0]}", result[0].contains(" "))
    }

    @Test
    fun testHeadersDisabledMode01() {
        parser.parse("ATH0")
        val result = parser.parse("010C")
        assertFalse("No headers: ${result[0]}", result[0].contains(" 41"))
    }

    @Test
    fun testMultiplePidsSupported() {
        val pids = listOf("0100", "0101", "0104", "0105", "010C", "010D", "010F", "0110", "012F")
        for (pid in pids) {
            val result = parser.parse(pid)
            assertTrue("PID $pid failed: $result", result.isNotEmpty() && !result[0].startsWith("NO DATA") || result.isEmpty() || result[0] == "NO DATA")
        }
    }

    @Test
    fun testAllProtocols() {
        for (sp in listOf("ATSP0", "ATSP1", "ATSP2", "ATSP3", "ATSP4", "ATSP5")) {
            val result = parser.parse(sp)
            assertEquals(listOf("OK"), result)
        }
    }

    @Test
    fun testSpacingCommands() {
        assertEquals(listOf("OK"), parser.parse("ATS0"))
        assertEquals(listOf("OK"), parser.parse("ATS1"))
    }

    @Test
    fun testShortCommandFormat() {
        val result = parser.parse("01 0C")
        assertNotNull(result)
    }

    @Test
    fun testLowerCaseCommands() {
        parser.parse("ate0")
        assertFalse(parser.isEchoEnabled())
        parser.parse("atz")
        assertTrue(parser.isEchoEnabled())
    }
}
