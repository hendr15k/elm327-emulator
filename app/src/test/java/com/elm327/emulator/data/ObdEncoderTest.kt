package com.elm327.emulator.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Direct tests for the encoders that fix the decimal-vs-hex and scaling bugs.
 * These run on the JVM with the real encoder functions - not on the Android
 * emulator, so every value is reproducible.
 */
class ObdEncoderTest {

    private fun decodeByte(hex: String): Int = hex.toInt(16)
    private fun decodeWord(hex: String): Int =
        hex.substring(0, 2).toInt(16) * 256 + hex.substring(2, 4).toInt(16)

    @Test
    fun speedIsOneHexByte() {
        assertEquals("64", ObdEncoder.speed(100))
        assertEquals("00", ObdEncoder.speed(0))
        assertEquals("FF", ObdEncoder.speed(255))
    }

    @Test
    fun coolantTemperatureRoundTrip() {
        // Celsius stored in the sim; wire byte = celsius + 40.
        assertEquals("32", ObdEncoder.temperature(10))   // 10 + 40 = 50 = 0x32
        assertEquals("64", ObdEncoder.temperature(60))   // 60 + 40 = 100 = 0x64
        assertEquals("28", ObdEncoder.temperature(0))    // 0 + 40 = 40 = 0x28
        assertEquals("00", ObdEncoder.temperature(-40))  // floor
        assertEquals("FF", ObdEncoder.temperature(215))  // 215 + 40 = 255 = 0xFF
    }

    @Test
    fun intakeTempUsesAPlus40NotHalfScale() {
        // Old bug: ((intake + 40) * 255 / 510).toInt() printed as decimal.
        assertEquals("5A", ObdEncoder.temperature(50)) // 50+40=90=0x5A
    }

    @Test
    fun rpmIsWordDividedByFour() {
        // PID 0x0C: ((256*A)+B)/4 rpm, so 600 rpm is 2400 = 0x0960 on the wire.
        assertEquals("09 60", ObdEncoder.rpm(600.0))
        assertEquals("1F 40", ObdEncoder.rpm(2000.0))
    }

    @Test
    fun engineLoadIsPercent8bit() {
        assertEquals("00", ObdEncoder.percent8bit(0.0))
        assertEquals("FF", ObdEncoder.percent8bit(100.0))
        assertEquals("7F", ObdEncoder.percent8bit(50.0))
    }

    @Test
    fun timingAdvanceRoundTrip() {
        // ((A/2)-64) degrees
        assertEquals("00", ObdEncoder.timingAdvance(-64.0)) // A=0 -> -64 deg
        assertEquals("80", ObdEncoder.timingAdvance(0.0))   // A=128 -> 0 deg
        assertEquals("FF", ObdEncoder.timingAdvance(63.5))  // A=255 -> 63.5 deg
    }

    @Test
    fun timingAdvanceHas64DegreeOffset() {
        assertEquals("00", ObdEncoder.timingAdvance(-64.0)) // -64+64=0 -> 0x00
    }

    @Test
    fun fuelTrimIsCenteredAt128() {
        assertEquals("80", ObdEncoder.fuelTrim(0.0))    // 128 + 0 = 128 = 0x80
        assertEquals("00", ObdEncoder.fuelTrim(-100.0)) // 128 - 128 = 0
        assertEquals("FF", ObdEncoder.fuelTrim(100.0))  // 128 + 128 = 255 = 0xFF
        assertEquals("73", ObdEncoder.fuelTrim(-10.0))  // 128 - 12.8 = 115.2 -> 0x73
    }

    @Test
    fun mafIsWordGramsPerSecondTimes100() {
        assertEquals("00 00", ObdEncoder.maf(0.0))
        assertEquals("00 C8", ObdEncoder.maf(2.0)) // 200 = 0x00C8
    }

    @Test
    fun o2SensorReturnsVoltageAndTrimBytes() {
        assertEquals("64 80", ObdEncoder.o2Sensor(0.5, 0.0)) // 0.5*200=100=0x64, trim 0x80
        assertEquals("C8 00", ObdEncoder.o2Sensor(1.0, -100.0))
    }

    @Test
    fun catalystTemperatureRoundTrip() {
        // ((256*A+B)/10) - 40
        assertEquals("19 00", ObdEncoder.catalystTemperature(600.0)) // (600+40)*10 = 6400 = 0x1900
        assertEquals("00 00", ObdEncoder.catalystTemperature(-40.0))
    }

    @Test
    fun equivalenceRatioIsDividedBy32768() {
        assertEquals("80 00", ObdEncoder.equivalenceRatio(1.0)) // 32768 = 0x8000
        assertEquals("00 00", ObdEncoder.equivalenceRatio(0.0))
    }

    @Test
    fun injectionTimingRoundTrip() {
        // ((256*A+B)/128) - 210
        assertEquals("00 00", ObdEncoder.injectionTiming(-210.0)) // 0 -> -210
        assertEquals("69 00", ObdEncoder.injectionTiming(0.0))    // (0+210)*128 = 26880 = 0x6900
    }

    @Test
    fun evapVaporPressureIsSigned() {
        // ((256*A+B)/4) - 8192 pascal
        assertEquals("00 00", ObdEncoder.evapVaporPressure(-8192)) // (-8192+8192)*4 = 0
        assertEquals("80 00", ObdEncoder.evapVaporPressure(0))   // (8192)*4 = 32768 = 0x8000
        assertEquals("FF FC", ObdEncoder.evapVaporPressure(8191)) // (16383)*4 = 65532 = 0xFFFC
    }

    @Test
    fun fuelRailPressureGaugeIsTenKpaPerBit() {
        assertEquals("03 E8", ObdEncoder.railPressureGauge(100.0)) // 100*10 = 1000 = 0x03E8
    }

    @Test
    fun supportedPidBitmapUsesMsbFirstOrder() {
        // base 0: PID 0x01 is bit 31 (MSB of byte A = 0x80), PID 0x08 is
        // bit 24 (LSB of byte A = 0x01).
        val bitmap = ObdEncoder.supportedPidBitmap(0x00, setOf(0x01, 0x08))
        assertEquals("81 00 00 00", bitmap)
        // A PID in the next range must land in the second byte: 0x09 -> bit 23.
        assertEquals("00 80 00 00", ObdEncoder.supportedPidBitmap(0x00, setOf(0x09)))
    }

    @Test
    fun allOutputIsHex() {
        val values = listOf(
            ObdEncoder.speed(100),
            ObdEncoder.temperature(10),
            ObdEncoder.rpm(600.0),
            ObdEncoder.percent8bit(50.0),
            ObdEncoder.timingAdvance(0.0),
            ObdEncoder.voltage(13.8),
            ObdEncoder.maf(2.5),
            ObdEncoder.o2Sensor(0.85),
            ObdEncoder.catalystTemperature(600.0),
            ObdEncoder.equivalenceRatio(1.0),
            ObdEncoder.injectionTiming(0.0),
            ObdEncoder.evapVaporPressure(0),
            ObdEncoder.railPressureGauge(325.0),
            ObdEncoder.supportedPidBitmap(0, setOf(0x01, 0x20))
        )
        for (s in values) {
            for (token in s.split(" ")) {
                if (token.isNotEmpty()) {
                    token.toInt(16)
                }
            }
        }
    }

    @Test
    fun formattingIsLocaleIndependent() {
        // A device with an Arabic locale would print Arabic-Indic digits; the
        // encoder must never depend on the default locale.
        val s = ObdEncoder.speed(255)
        assertEquals("FF", s)
        assertTrue(s.all { it in '0'..'9' || it in 'A'..'F' })
    }
}
