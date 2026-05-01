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
        assertEquals(listOf("12.0V"), parser.parse("ATRV"))
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
        assertEquals(listOf(">"), parser.parse(""))
        assertEquals(listOf(">"), parser.parse("  "))
        assertEquals(listOf(">"), parser.parse("\r"))
    }

    @Test
    fun testRpmResponse() {
        val result = parser.parse("010C\r")
        assertNotNull(result)
        assertTrue(result.size == 1)
        val response = result[0]
        assertTrue("Response should contain RPM data, got: $response", response.matches(Regex("410C[0-9A-F ]+")))
    }

    @Test
    fun testSpeedResponse() {
        val result = parser.parse("010D")
        assertNotNull(result)
        val response = result[0]
        assertTrue("Speed response: $response", response.matches(Regex("410D\\d+")))
    }

    @Test
    fun testCoolantTemp() {
        val result = parser.parse("0105")
        assertNotNull(result)
        val temp = result[0].replace("4105", "").toInt()
        assertTrue("Coolant temp should be >= -40, got $temp", temp >= -40)
        assertTrue("Coolant temp should be <= 215, got $temp", temp <= 215)
    }

    @Test
    fun testEngineLoad() {
        val result = parser.parse("0104")
        assertNotNull(result)
        assertTrue("Engine load: ${result[0]}", result[0].matches(Regex("4104[0-9A-F]{2}")))
    }

    @Test
    fun testThrottlePosition() {
        val result = parser.parse("010B")
        assertNotNull(result)
        val response = result[0]
        assertTrue("Throttle: $response", response.matches(Regex("410B\\d+")))
    }

    @Test
    fun testIntakeTemp() {
        val result = parser.parse("010F")
        assertNotNull(result)
        val temp = result[0].replace("410F", "").toInt()
        assertTrue("Intake temp should be >= 0, got $temp", temp >= 0)
    }

    @Test
    fun testMafRate() {
        val result = parser.parse("0110")
        assertNotNull(result)
        assertTrue("MAF: ${result[0]}", result[0].matches(Regex("4110[0-9A-F ]+")))
    }

    @Test
    fun testFuelLevel() {
        val result = parser.parse("012F")
        assertNotNull(result)
        assertTrue("Fuel: ${result[0]}", result[0].matches(Regex("412F\\d+")))
    }

    @Test
    fun testRuntime() {
        val result = parser.parse("011F")
        assertNotNull(result)
        assertTrue("Runtime: ${result[0]}", result[0].matches(Regex("411F\\d+")))
    }

    @Test
    fun testRuntimeBytes() {
        val result = parser.parse("0121")
        assertNotNull(result)
        assertTrue("Runtime bytes: ${result[0]}", result[0].matches(Regex("4121\\d+ \\d+")))
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
    fun testUnknownPid() {
        val result = parser.parse("0199")
        assertEquals(listOf("NO DATA"), result)
    }

    @Test
    fun testUnknownMode() {
        assertEquals(listOf("?"), parser.parse("0900"))
    }

    @Test
    fun testSupportedPids0100() {
        val result = parser.parse("0100")
        assertEquals(listOf("4100BE 3E A8 13"), result)
    }

    @Test
    fun testHeadersEnabledMode01() {
        parser.parse("ATH1")
        val result = parser.parse("010C")
        assertTrue("Headers: ${result[0]}", result[0].contains(" "))
    }

    @Test
    fun testFuelType() {
        val result = parser.parse("0151")
        assertNotNull(result)
        assertTrue("Fuel type: ${result[0]}", result[0].contains("04"))
    }

    @Test
    fun testBarometricPressure() {
        val result = parser.parse("0133")
        assertNotNull(result)
    }

    @Test
    fun testRpmRange() {
        for (i in 1..10) {
            val result = parser.parse("010C")
            val hex = result[0].replace("410C", "").replace(" ", "")
            val a = hex.substring(0, 2).toInt(16)
            val b = hex.substring(2, 4).toInt(16)
            val rpm = (a * 256 + b) / 4.0
            assertTrue("RPM $rpm should be in range 50-15000", rpm in 50.0..15000.0)
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
        val temp = result[0].replace("410F", "").toInt()
        assertTrue("Intake temp after reset should be (30+40)*255/510 = ~35, got $temp", temp in 34..36)
    }

    @Test
    fun testFuelLevelNotOver100() {
        for (i in 1..1000) {
            parser.parse("012F")
        }
        val result = parser.parse("012F")
        val fuelVal = result[0].replace("412F", "").toInt()
        assertTrue("Fuel value should be 0-255, got $fuelVal", fuelVal in 0..255)
    }
}
