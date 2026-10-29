package com.elm327.emulator.data

import java.util.Locale

/**
 * Turns simulated physical values into the hexadecimal byte pairs that an
 * ELM327 puts on the wire.
 *
 * Two rules that every OBD-II client depends on:
 *  - a payload byte is always two hex digits ("64" = 100 km/h). Decimal output
 *    such as "100" is not a valid byte and makes clients misread or drop the
 *    frame.
 *  - hex formatting must not depend on the device locale, otherwise a device
 *    with non-latin digits prints garbage.
 */
internal object ObdEncoder {

    fun byte(value: Int): String = String.format(Locale.US, "%02X", value.coerceIn(0, 0xFF))

    fun word(value: Int): String {
        val clamped = value.coerceIn(0, 0xFFFF)
        return String.format(Locale.US, "%02X %02X", clamped / 256, clamped % 256)
    }

    /** PID 0x0C: ((A*256)+B) / 4 rpm */
    fun rpm(rpm: Double): String = word((rpm * 4).toInt())

    /** PID 0x0D: A km/h */
    fun speed(kmh: Int): String = byte(kmh)

    /** PIDs 0x05, 0x0F, 0x46, 0x5C: A - 40 °C */
    fun temperature(celsius: Int): String = byte(celsius + 40)

    /** PIDs 0x06-0x09: (A - 128) * 100 / 128 percent. */
    fun fuelTrim(percent: Double): String = byte((128 + percent * 128 / 100).toInt())

    /** PIDs 0x04/0x11/0x2C/0x2E/0x2F/0x45/0x47..0x4C: A * 100 / 255 percent. */
    fun percent8bit(percent: Double): String = byte((percent * 255 / 100).toInt())

    /** PID 0x0E: A / 2 - 64 degrees before top dead centre. */
    fun timingAdvance(degrees: Double): String = byte(((degrees + 64) * 2).toInt())

    /** PID 0x42: ((256*A)+B)/1000 volt. */
    fun voltage(volts: Double): String = word((volts * 1000).toInt())

    /** PID 0x10: ((256*A)+B)/100 grams per second. */
    fun maf(gramsPerSecond: Double): String = word((gramsPerSecond * 100).toInt())

    /** PIDs 0x14-0x1B: A/200 volt plus a short term fuel trim byte. */
    fun o2Sensor(volts: Double, trimPercent: Double = 0.0): String =
        byte((volts * 200).toInt()) + " " + fuelTrim(trimPercent)

    /** PIDs 0x3C-0x3E: ((256*A)+B)/10 - 40 °C. */
    fun catalystTemperature(celsius: Double): String = word(((celsius + 40) * 10).toInt())

    /** PID 0x44: ((256*A)+B)/32768 (stoichiometric ratio, 1.0 = 32768). */
    fun equivalenceRatio(ratio: Double): String = word((ratio * 32768).toInt())

    /** PID 0x5D: ((256*A)+B)/128 - 210 degrees before top dead centre. */
    fun injectionTiming(degrees: Double): String = word(((degrees + 210) * 128).toInt())

    /** PID 0x32: ((256*A)+B)/4 - 8192 pascal (signed). */
    fun evapVaporPressure(pascal: Int): String = word((pascal + 8192) * 4)

    /** PID 0x2F/0x43: percentage scaled to 0..255. */
    fun absoluteLoad(percent: Double): String = word((percent * 255 / 100).toInt())

    /** PID 0x23: 10 kPa per bit. */
    fun railPressureGauge(kPa: Double): String = word((kPa * 10).toInt())

    /** PID 0x22: 0.079 kPa per bit. */
    fun railPressureVacuum(kPa: Double): String = word((kPa / 0.079).toInt())

    /** PID 0x31/0x21: ((256*A)+B) kilometres. */
    fun distance(km: Int): String = word(km)

    /** PID 0x1F/0x30/0x4D/0x4E: ((256*A)+B) seconds or minutes. */
    fun runtime(value: Int): String = word(value)

    /**
     * Bitmap of the PIDs supported in the 32-PID range that starts at [base]
     * (0x00, 0x20, 0x40, ...). Bit 31 of the mask belongs to base+1, bit 0 to
     * base+0x20 - that is the order SAE J1979 defines, MSB first.
     */
    fun supportedPidBitmap(base: Int, supported: Set<Int>): String {
        var mask = 0L
        for (offset in 1..32) {
            if ((base + offset) in supported) {
                mask = mask or (1L shl (32 - offset))
            }
        }
        return String.format(
            Locale.US,
            "%02X %02X %02X %02X",
            ((mask shr 24) and 0xFF).toInt(),
            ((mask shr 16) and 0xFF).toInt(),
            ((mask shr 8) and 0xFF).toInt(),
            (mask and 0xFF).toInt()
        )
    }
}
