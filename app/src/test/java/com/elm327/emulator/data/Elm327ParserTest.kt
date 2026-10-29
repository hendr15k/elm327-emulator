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

    private fun hexPayload(line: String): String = line.replace(" ", "")

    private fun decodeSpeed(response: String): Int {
        val payload = hexPayload(response).removePrefix("410D")
        return payload.substring(0, 2).toInt(16)
    }

    private fun decodeRpm(response: String): Double {
        val payload = hexPayload(response).removePrefix("410C")
        val a = payload.substring(0, 2).toInt(16)
        val b = payload.substring(2, 4).toInt(16)
        return (a * 256 + b) / 4.0
    }

    private fun decodeTemperature(response: String, pid: String): Int {
        val payload = hexPayload(response).removePrefix("41$pid")
        return payload.substring(0, 2).toInt(16) - 40
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
        assertEquals(listOf("AUTO"), parser.parse("ATDP"))
        assertEquals(listOf("A0"), parser.parse("ATDPN"))
    }

    @Test
    fun testProtocolSet() {
        assertEquals(listOf("OK"), parser.parse("ATSP6"))
        assertEquals(listOf("ISO 15765-4 (CAN 11/500)"), parser.parse("ATDP"))
    }

    @Test
    fun testProtocolAutoCan() {
        parser.parse("ATSPA6")
        assertEquals(listOf("AUTO, ISO 15765-4 (CAN 11/500)"), parser.parse("ATDP"))
        assertEquals(listOf("A6"), parser.parse("ATDPN"))
        parser.parse("ATSPAA")
        assertEquals(listOf("AUTO, SAE J1939 (CAN 29/250)"), parser.parse("ATDP"))
    }

    @Test
    fun testProtocolNumbersFollowDatasheet() {
        // Datasheet numbering: 4 = KWP 5 baud init, 5 = KWP fast init,
        // 6/7/8/9 = CAN, A/B/C = J1939 / USER1 / USER2.
        parser.parse("ATSP4")
        assertEquals(listOf("ISO 14230-4 (KWP 5BAUD)"), parser.parse("ATDP"))
        parser.parse("ATSP5")
        assertEquals(listOf("ISO 14230-4 (KWP FAST)"), parser.parse("ATDP"))
        parser.parse("ATSP7")
        assertEquals(listOf("ISO 15765-4 (CAN 29/500)"), parser.parse("ATDP"))
        parser.parse("ATSPAA")
        assertEquals(listOf("AUTO, SAE J1939 (CAN 29/250)"), parser.parse("ATDP"))
    }

    @Test
    fun testInvalidProtocolRejected() {
        assertEquals(listOf("?"), parser.parse("ATSPD"))
        assertEquals(listOf("?"), parser.parse("ATSP13"))
    }

    @Test
    fun testDeviceInfo() {
        assertEquals(listOf("ELM327 v1.5"), parser.parse("ATI"))
        assertEquals(listOf("ELM327 v1.5"), parser.parse("ATI1"))
        assertEquals(listOf("ELM327"), parser.parse("ATI0"))
        assertEquals(listOf("ELM327"), parser.parse("AT@2"))
    }

    @Test
    fun testVoltageReflectsSimulator() {
        val result = parser.parse("ATRV")
        val volts = result[0].removeSuffix("V").toDouble()
        assertEquals(VehicleSimulator.getBatteryVoltage(), volts, 0.06)
    }

    @Test
    fun testDisplayProtocol() {
        parser.parse("ATSP6")
        assertEquals(listOf("ISO 15765-4 (CAN 11/500)"), parser.parse("ATDP"))
    }

    @Test
    fun testDisplayProtocolNumber() {
        parser.parse("ATSP6")
        assertEquals(listOf("6"), parser.parse("ATDPN"))
    }

    @Test
    fun testTimeout() {
        assertEquals(listOf("OK"), parser.parse("ATSTFF"))
    }

    @Test
    fun testTimeoutWithValue() {
        // ATST64 broke the parser before because the fixed command table had
        // no entry for it; the chip accepts the hex timeout inline.
        assertEquals(listOf("OK"), parser.parse("ATST64"))
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
        assertTrue("RPM out of range", decodeRpm(response) in 600.0..7000.0)
    }

    @Test
    fun testSpeedResponse() {
        val result = parser.parse("010D")
        assertNotNull(result)
        assertTrue("Speed response: $result", result[0].startsWith("41"))
        assertTrue("Speed out of range", decodeSpeed(result[0]) in 0..255)
    }

    @Test
    fun testCoolantTempDecodesToSimulatorRange() {
        val result = parser.parse("0105")
        assertNotNull(result)
        assertTrue("Coolant: ${result[0]}", result[0].startsWith("41"))
        assertTrue(
            "Coolant should decode to -40..215",
            decodeTemperature(result[0], "05") in -40..215
        )
    }

    @Test
    fun testEngineLoad() {
        val result = parser.parse("0104")
        assertNotNull(result)
        assertTrue("Engine load: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testEngineLoadRoundTrip() {
        // A load byte of "100" (decimal output, the old bug) would parse here
        // as 0x10 = 16/2.55 = 6.3 percent instead of the intended 39.2 percent.
        for (i in 1..20) {
            val payload = hexPayload(parser.parse("0104")[0]).removePrefix("4104")
            val percent = payload.substring(0, 2).toInt(16) * 100.0 / 255.0
            assertTrue("Engine load percent $percent out of range", percent in 0.0..100.0)
        }
    }

    @Test
    fun testThrottlePosition() {
        val result = parser.parse("010B")
        assertNotNull(result)
        assertTrue("Throttle: ${result[0]}", result[0].startsWith("41"))
        // Intake manifold pressure (PID 0x0B) shares the single-byte scaling,
        // so it must be one hex byte pair, not a decimal number.
        val payload = hexPayload(result[0]).removePrefix("410B")
        assertEquals("PID 0x0B must be one byte pair, got: $payload", 2, payload.length)
        payload.substring(0, 2).toInt(16)
    }

    @Test
    fun testIntakeTemp() {
        val result = parser.parse("010F")
        assertNotNull(result)
        assertTrue("Intake temp: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testIntakeTempUsesStandardScaling() {
        // Decoded value must match the simulator within the +-1 drift of
        // update(); the old half-scale formula decoded 30 C as 13 C.
        val decoded = decodeTemperature(parser.parse("010F")[0], "0F")
        assertEquals(VehicleSimulator.getIntakeAirTemperature().toDouble(), decoded.toDouble(), 2.0)
    }

    @Test
    fun testAmbientTempUsesStandardScaling() {
        val decoded = decodeTemperature(parser.parse("0146")[0], "46")
        assertEquals(VehicleSimulator.getAmbientAirTemperature().toDouble(), decoded.toDouble(), 2.0)
    }

    @Test
    fun testOilTempUsesStandardScaling() {
        val decoded = decodeTemperature(parser.parse("015C")[0], "5C")
        assertEquals(VehicleSimulator.getEngineOilTemperature().toDouble(), decoded.toDouble(), 2.0)
    }

    @Test
    fun testCatalystTempUsesStandardScaling() {
        val payload = hexPayload(parser.parse("013C")[0]).removePrefix("413C")
        val celsius = (payload.substring(0, 2).toInt(16) * 256 + payload.substring(2, 4).toInt(16)) / 10.0 - 40.0
        // Noise is +-20 K, so 25 K tolerance still catches the old formula,
        // which reported about -15 C for a 500 C catalyst.
        assertEquals(VehicleSimulator.getCatalystTemperature(), celsius, 25.0)
    }

    @Test
    fun testSpeedMatchesSimulator() {
        val decoded = decodeSpeed(parser.parse("010D")[0])
        assertEquals(VehicleSimulator.getVehicleSpeed().toDouble(), decoded.toDouble(), 3.0)
    }

    @Test
    fun testRpmMatchesSimulator() {
        val decoded = decodeRpm(parser.parse("010C")[0])
        assertEquals(VehicleSimulator.getEngineSpeed(), decoded, 51.0)
    }

    @Test
    fun testMafRate() {
        val result = parser.parse("0110")
        assertNotNull(result)
        assertTrue("MAF: ${result[0]}", result[0].startsWith("41"))
        val payload = hexPayload(result[0]).removePrefix("4110")
        assertEquals(4, payload.length)
    }

    @Test
    fun testMafRateRoundTrip() {
        val payload = hexPayload(parser.parse("0110")[0]).removePrefix("4110")
        val grams = (payload.substring(0, 2).toInt(16) * 256 + payload.substring(2, 4).toInt(16)) / 100.0
        assertTrue("MAF $grams g/s out of range", grams in 0.0..655.35)
    }

    @Test
    fun testFuelLevel() {
        val result = parser.parse("012F")
        assertNotNull(result)
        assertTrue("Fuel: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testFuelLevelIsValidHexByte() {
        for (i in 1..50) {
            val payload = hexPayload(parser.parse("012F")[0]).removePrefix("412F")
            assertEquals("Fuel must be one hex byte, got: $payload", 2, payload.length)
            assertTrue("Fuel byte out of range", payload.toInt(16) in 0..255)
        }
    }

    @Test
    fun testRuntime() {
        val result = parser.parse("011F")
        assertNotNull(result)
        assertTrue("Runtime: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testRuntimeRoundTrip() {
        val payload = hexPayload(parser.parse("011F")[0]).removePrefix("411F")
        assertEquals(4, payload.length)
        val seconds = payload.substring(0, 2).toInt(16) * 256 + payload.substring(2, 4).toInt(16)
        assertTrue("Runtime should be two bytes, got $seconds from $payload", seconds in 0..65535)
    }

    @Test
    fun testRuntimeBytes() {
        val result = parser.parse("0121")
        assertNotNull(result)
        assertTrue("Runtime bytes: ${result[0]}", result[0].startsWith("41"))
        val payload = hexPayload(result[0]).removePrefix("4121")
        assertEquals(4, payload.length)
        payload.substring(0, 2).toInt(16)
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
    fun testMode0A() {
        val result = parser.parse("0A")
        assertNotNull(result)
        assertTrue("Mode 0A: ${result[0]}", result[0].startsWith("4A"))
        val payload = hexPayload(result[0]).removePrefix("4A")
        assertEquals(8, payload.length)
    }

    @Test
    fun testMode04() {
        val result = parser.parse("04")
        assertEquals(listOf("44"), result)
    }

    @Test
    fun testMode05() {
        val result = parser.parse("05")
        assertTrue("Mode 05: ${result[0]}", result[0].startsWith("45"))
    }

    @Test
    fun testMode06() {
        val result = parser.parse("06")
        assertTrue("Mode 06: ${result[0]}", result[0].startsWith("46"))
    }

    @Test
    fun testMode08() {
        val result = parser.parse("08")
        assertTrue("Mode 08: ${result[0]}", result[0].startsWith("48"))
    }

    @Test
    fun testMode08DataLength() {
        val result = parser.parse("08")
        val payload = hexPayload(result[0]).removePrefix("48")
        assertTrue("Mode 08 reply should carry one control byte, got: $payload", payload.length >= 2)
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
    fun testVinHas17Characters() {
        val payload = hexPayload(parser.parse("0904")[0]).removePrefix("4904")
        val vinBytes = payload.chunked(2)
        assertEquals("VIN payload without length byte", 18, vinBytes.size)
        val vin = vinBytes.drop(1).map { it.toInt(16).toChar() }.joinToString("")
        assertEquals(17, vin.length)
        assertFalse("VIN must not contain a padding zero", '\u0000' in vin)
    }

    @Test
    fun testMode09CalId() {
        val result = parser.parse("090A")
        assertTrue("Mode 09 Cal ID: ${result[0]}", result[0].startsWith("49 0A"))
    }

    @Test
    fun testMode09UnknownPidReturnsNoData() {
        assertEquals(listOf("NO DATA"), parser.parse("0908"))
    }

    @Test
    fun testMode02() {
        val result = parser.parse("0202")
        assertTrue("Mode 02: ${result[0]}", result[0].startsWith("42"))
    }

    @Test
    fun testMode02FrameNumber() {
        val withFrame = hexPayload(parser.parse("020502")[0])
        val withoutFrame = hexPayload(parser.parse("0205")[0])
        val frameByte = withFrame.removePrefix("4205").substring(0, 2)
        assertEquals("02", frameByte)
        assertTrue(withoutFrame.removePrefix("4205").startsWith("00"))
    }

    @Test
    fun testMode02UnknownPidReturnsNoData() {
        assertEquals(listOf("NO DATA"), parser.parse("023C"))
    }

    @Test
    fun testUnknownPid() {
        val result = parser.parse("01FF")
        assertEquals(listOf("NO DATA"), result)
    }

    @Test
    fun testMode01RangeMarketsAdvertiseNextRange() {
        // 0120/0140 used to return NO DATA even though higher PIDs were
        // advertised in the bitmap; poll loops that walk 00 -> 20 -> 40
        // would stop at the first gap.
        val r20 = parser.parse("0120")
        assertTrue("0120 should exist: $r20", r20[0].startsWith("41 20"))
        assertEquals(8, hexPayload(r20[0]).removePrefix("4120").length)
        val r40 = parser.parse("0140")
        assertTrue("0140 should exist: $r40", r40[0].startsWith("41 40"))
        val r60 = parser.parse("0160")
        assertTrue("0160 should be NO DATA on this vehicle: $r60", r60 == listOf("NO DATA"))
        val r80 = parser.parse("0180")
        assertTrue("0180 should be NO DATA on this vehicle: $r80", r80 == listOf("NO DATA"))
    }

    @Test
    fun testMode01UnsupportedPidNotAdvertised() {
        for (pid in listOf("0102", "0112", "0118", "0124", "0134", "0153")) {
            assertEquals("PID $pid must not be advertised", listOf("NO DATA"), parser.parse(pid))
        }
    }

    @Test
    fun testSupportedPidBitmapIsInternallyConsistent() {
        val supported = parser.parse("0100")[0].replace(" ", "").removePrefix("4100")
        for (offset in 1..32) {
            val pid = "01%02X".format(offset)
            val advertised = (supported.toLong(16) and (1L shl (32 - offset))) != 0L
            val answered = parser.parse(pid)[0] != "NO DATA"
            // The 0x20 marker owns the range bit but is implemented separately.
            val implemented = offset == 0x20 || answered
            assertEquals("Bitmap disagrees with parser for $pid", advertised, implemented)
        }
    }

    @Test
    fun testMode01ResponsesFitPidLength() {
        // Two-byte PIDs ran through the one-byte timing formula before, which
        // truncated e.g. 0x011F to a single byte.
        mapOf(
            "0104" to 2, "0105" to 2, "010C" to 4, "010D" to 2, "010F" to 2,
            "0110" to 4, "011F" to 4, "0121" to 4, "012F" to 2, "0142" to 4, "0146" to 2
        ).forEach { (pid, hexChars) ->
            val payload = hexPayload(parser.parse(pid)[0])
                .removePrefix("41" + pid.substring(2))
            assertEquals("$pid payload length", hexChars, payload.length)
        }
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
    fun testFuelTypeMatchesObdRatingTable() {
        // 0152 (ethanol) was mapped to fuel trim and "Diesel" (0151 -> 0x04)
        // to an absolute-throttle ratio; the J1979 table says 1 = gasoline.
        parser.reset()
        parser.parse("ATS1")
        assertEquals(listOf("41 51 01"), parser.parse("0151"))
        val ethanol = hexPayload(parser.parse("0152")[0]).removePrefix("4152")
        assertTrue("Ethanol must decode 0..100 percent: $ethanol", ethanol.toInt(16) in 0..100)
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
        assertEquals(2, hexPayload(result[0]).removePrefix("4133").length)
    }

    @Test
    fun testControlModuleVoltage() {
        val result = parser.parse("0142")
        assertNotNull(result)
        assertTrue("Voltage: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testControlModuleVoltageRoundTrip() {
        val payload = hexPayload(parser.parse("0142")[0]).removePrefix("4142")
        assertEquals(4, payload.length)
        val volts = (payload.substring(0, 2).toInt(16) * 256 + payload.substring(2, 4).toInt(16)) / 1000.0
        assertEquals(VehicleSimulator.getBatteryVoltage(), volts, 0.002)
    }

    @Test
    fun testAbsoluteLoadRange() {
        val payload = hexPayload(parser.parse("0143")[0]).removePrefix("4143")
        assertEquals(4, payload.length)
    }

    @Test
    fun testAbsoluteLoadRoundTrip() {
        val payload = hexPayload(parser.parse("0143")[0]).removePrefix("4143")
        val raw = payload.substring(0, 2).toInt(16) * 256 + payload.substring(2, 4).toInt(16)
        assertTrue("Absolute load must fit one scaled byte: $raw", raw in 0..255)
    }

    @Test
    fun testCatalystTempRoundTrip() {
        val payload = hexPayload(parser.parse("013C")[0]).removePrefix("413C")
        val celsius = (payload.substring(0, 2).toInt(16) * 256 + payload.substring(2, 4).toInt(16)) / 10.0 - 40.0
        assertTrue("Catalyst temp $celsius out of range", celsius in -40.0..6513.5)
    }

    @Test
    fun testAmbientTemp() {
        val result = parser.parse("0146")
        assertNotNull(result)
        assertEquals(2, hexPayload(result[0]).removePrefix("4146").length)
    }

    @Test
    fun testAmbientTempDecodesToSimulatorRange() {
        assertTrue(
            "Ambient temp should decode to -40..215",
            decodeTemperature(parser.parse("0146")[0], "46") in -40..215
        )
    }

    @Test
    fun testOilTempAvailable() {
        val result = parser.parse("015C")
        assertTrue("Oil temp: ${result[0]}", result[0].startsWith("41 5C"))
        assertTrue(
            "Oil temp should decode to -40..215",
            decodeTemperature(result[0], "5C") in -40..215
        )
    }

    @Test
    fun testCommandedThrottle() {
        val result = parser.parse("0145")
        assertNotNull(result)
    }

    @Test
    fun testTimingAdvanceRoundTrip() {
        val payload = hexPayload(parser.parse("010E")[0]).removePrefix("410E")
        val degrees = payload.substring(0, 2).toInt(16) / 2.0 - 64.0
        assertTrue("Timing advance $degrees out of range", degrees in -64.0..63.5)
    }

    @Test
    fun testFuelTrimScaleIsCentered() {
        // J1979: (A - 128) * 100 / 128 percent. 0 percent must encode as 0x80,
        // not as (0 + 100) * 1.28 = 128... which the old formula never reached
        // because it read percent*1.28+100=128 for 0%... it did, but -100% gave
        // 0% and 100% saturated at 256. Check the boundaries explicitly.
        assertEquals("80", ObdEncoder.fuelTrim(0.0))
        assertEquals("00", ObdEncoder.fuelTrim(-100.0))
        assertEquals("FF", ObdEncoder.fuelTrim(100.0))
        // -10 percent must be 0x80 - 12.8 = 0x73.
        assertEquals("73", ObdEncoder.fuelTrim(-10.0))
    }

    @Test
    fun testFuelPressureRoundTrip() {
        val payload = hexPayload(parser.parse("010A")[0]).removePrefix("410A")
        assertEquals(2, payload.length)
        assertTrue(payload.substring(0, 2).toInt(16) in 0..255)
    }

    @Test
    fun testRpmRange() {
        for (i in 1..10) {
            val result = parser.parse("010C")
            assertTrue("RPM out of range", decodeRpm(result[0]) in 600.0..7000.0)
        }
    }

    @Test
    fun testHeadersToggle() {
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

        assertTrue("Headers ON: ${withHeaders[0]}", withHeaders[0].contains("41 0C"))
        assertTrue(
            "No headers: ${noHeaders[0]}",
            noHeaders[0].startsWith("41 0C") && !noHeaders[0].contains("7E8")
        )
        assertTrue("on=$on off=$off", on && off)
    }

    @Test
    fun testHeadersAddCanAddressing() {
        parser.parse("ATSP6")
        parser.parse("ATH1")
        val withHeaders = parser.parse("010C")
        assertTrue("CAN header missing: ${withHeaders[0]}", withHeaders[0].startsWith("7E8"))
        parser.parse("ATH0")
        assertTrue(parser.parse("010C")[0].startsWith("41 0C"))
    }

    @Test
    fun testCanLengthByteCountsServicePidAndPayload() {
        // Reference frame from a real adapter: "7E8 06 41 00 BE 3F A8 13" -
        // the length byte covers service + PID + four payload bytes.
        parser.parse("ATH1")
        parser.parse("ATS1")
        assertEquals(true, parser.parse("0100")[0].startsWith("7E8 06 41 00"))
        // 010C: service + PID + two payload bytes = 4
        val rpmFrame = parser.parse("010C")[0]
        assertEquals(true, rpmFrame.startsWith("7E8 04 41 0C"))
        // 0904 (VIN): service + PID + NODI + 17 VIN bytes = 20 = 0x14
        val vinFrame = parser.parse("0904")[0]
        assertEquals(true, vinFrame.startsWith("7E8 14 49 04"))
    }

    @Test
    fun testSpacingToggleControlsSeparators() {
        parser.parse("ATS1")
        assertTrue(parser.parse("010C")[0].contains(" "))
        parser.parse("ATS0")
        val compact = parser.parse("010C")
        assertFalse("ATS0 should remove separators: ${compact[0]}", " " in compact[0])
        parser.parse("ATS1")
        assertTrue(parser.parse("010C")[0].contains(" "))
    }

    @Test
    fun testFormatReplyAssemblesWireFormat() {
        parser.parse("ATE1")
        // Two message lines must each be CR terminated and must not be glued
        // onto each other; the prompt is CR '>' after a final CR.
        val twoLines = parser.formatReply("0100", listOf("41 00 BE 3E B8 13", "41 20 00 00 00 00"))
        assertEquals("0100\r41 00 BE 3E B8 13\r41 20 00 00 00 00\r\r>", twoLines)

        parser.parse("ATE0")
        assertEquals("ELM327 v1.5\r\r>", parser.formatReply("ATI", parser.parse("ATI")))

        // ATL1 without echo: LF after every CR, prompt still CR LF '>'.
        parser.parse("ATL1")
        assertEquals("ELM327 v1.5\r\n\r\n>", parser.formatReply("ATI", listOf("ELM327 v1.5")))

        // ATL1 with echo: the echoed command gets its own terminated line.
        parser.parse("ATE1")
        assertEquals("ATI\r\nELM327 v1.5\r\n\r\n>", parser.formatReply("ATI", listOf("ELM327 v1.5")))

        parser.parse("ATL0")
        assertEquals("ATI\rELM327 v1.5\r\r>", parser.formatReply("ATI", listOf("ELM327 v1.5")))

        // A bare CR (empty command, no responses) only re-prompts.
        assertEquals("\r>", parser.formatReply("", emptyList()))
    }

    @Test
    fun testIntakeTempAfterReset() {
        parser.reset()
        val result = parser.parse("010F")
        assertTrue("Intake temp: ${result[0]}", result[0].startsWith("41"))
    }

    @Test
    fun testFuelLevelRange() {
        for (i in 1..50) {
            val payload = hexPayload(parser.parse("012F")[0]).removePrefix("412F")
            assertEquals(2, payload.length)
            assertTrue("Fuel value out of range", payload.toInt(16) in 0..255)
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
            assertEquals("PID $pid failed: $result", "41 ${pid.substring(2)}", result[0].take(5))
        }
    }

    @Test
    fun testAllProtocols() {
        for (sp in listOf("ATSP0", "ATSP1", "ATSP2", "ATSP3", "ATSP4", "ATSP5", "ATSP6")) {
            val result = parser.parse(sp)
            assertEquals(listOf("OK"), result)
        }
    }

    @Test
    fun testSpacingCommands() {
        assertEquals(listOf("OK"), parser.parse("ATS0"))
        assertFalse(parser.isSpacingEnabled())
        assertEquals(listOf("OK"), parser.parse("ATS1"))
        assertTrue(parser.isSpacingEnabled())
    }

    @Test
    fun testShortCommandFormat() {
        val result = parser.parse("01 0C")
        assertNotNull(result)
        assertTrue("Spaced OBD command: $result", result[0].startsWith("41"))
    }

    @Test
    fun testLowerCaseCommands() {
        parser.parse("ate0")
        assertFalse(parser.isEchoEnabled())
        parser.parse("atz")
        assertTrue(parser.isEchoEnabled())
    }

    @Test
    fun testConfigCommandsAccepted() {
        for (cmd in listOf("ATCAF1", "ATSH7E0", "ATAT1", "ATR1", "ATD1", "ATFI", "ATPPS")) {
            assertEquals("Command $cmd should not fail: ", listOf("OK"), parser.parse(cmd))
        }
    }

    @Test
    fun testIgnitionStatus() {
        assertEquals(listOf("ON"), parser.parse("ATIGN"))
    }

    @Test
    fun testMultilineMode03Length() {
        val parts = hexPayload(parser.parse("03")[0]).removePrefix("43")
        assertEquals(8, parts.length)
    }
}
