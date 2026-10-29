package com.elm327.emulator.data

import kotlin.random.Random

/**
 * Simulated vehicle state and its OBD-II (SAE J1979) encoding.
 *
 * Only PIDs whose scaling is implemented correctly are modelled, and exactly
 * those are advertised in the supported-PID bitmap. Advertising a PID that is
 * not implemented, or answering with a value that is not a hex byte pair,
 * makes clients show wrong numbers.
 */
object VehicleSimulator {

    private var engineRpm = 850.0
    private var vehicleSpeed = 0
    private var coolantTemp = 80
    private var intakeTemp = 30
    private var ambientTemp = 25
    private var engineOilTemp = 90
    private var throttlePosition = 0.0
    private var engineLoad = 15.0
    private var absoluteLoadValue = 15.0
    private var relativeThrottle = 0.0
    private var fuelLevel = 75.0
    private var fuelType = 0x01
    private var ethanolPercent = 0.0
    private var fuelPressure = 325
    private var intakeManifoldPressure = 40
    private var barometricPressure = 101
    private var evapSystemVaporPressure = 0
    private var timingAdvance = 10.0
    private var mafRate = 2.5
    private var fuelInjectionTiming = 0.0
    private var engineFuelRate = 2.5
    private var oxygenSensor1 = 0.85
    private var oxygenSensor2 = 0.72
    private var oxygenSensor3 = 0.90
    private var oxygenSensor4 = 0.65
    private var fuelTrimShortTerm1 = 0.0
    private var fuelTrimShortTerm2 = 0.0
    private var fuelTrimLongTerm1 = 0.0
    private var fuelTrimLongTerm2 = 0.0
    private var controlModuleVoltage = 13.8
    private var absoluteThrottleB = 0.0
    private var absoluteThrottleC = 0.0
    private var acceleratorPedalD = 0.0
    private var acceleratorPedalE = 0.0
    private var acceleratorPedalF = 0.0
    private var commandedEgr = 5.0
    private var egrError = 0.0
    private var evaporativePurge = 25.0
    private var warmUpsSinceCodesCleared = 127
    private var distanceSinceCodesCleared = 15234
    private var runtime = 1200
    private var timeWithMilOn = 0
    private var timeSinceCodesCleared = 120
    private var catalystTempBank1Sensor1 = 600.0
    private var catalystTempBank2Sensor1 = 580.0
    private var fuelSystemStatus = 0x02

    private val dtcs = mutableListOf<String>()
    private val pendingDtcs = mutableListOf<String>()

    private val vin = "1HGBH41JXMN109186"

    /** PIDs that [getMode01Response] answers with correctly scaled data. */
    private val modeledPids: Set<Int> = setOf(
        0x01, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
        0x10, 0x11, 0x14, 0x15, 0x16, 0x17, 0x1C, 0x1F, 0x21, 0x23, 0x2C, 0x2D, 0x2E, 0x2F,
        0x30, 0x31, 0x32, 0x33, 0x3C, 0x3D, 0x3E, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47,
        0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x51, 0x52, 0x5C, 0x5D, 0x5E
    )

    /**
     * PIDs advertised in the bitmap: the modelled ones plus the "range marker"
     * PIDs (0x20, 0x40, ...) that tell a client the next range has data.
     */
    private val advertisedPids: Set<Int> = modeledPids + listOf(0x20, 0x40, 0x60).filter { marker ->
        modeledPids.any { it > marker && it <= marker + 0x20 }
    }

    fun initialize() {
        engineRpm = 850.0
        vehicleSpeed = 0
        coolantTemp = 80
        intakeTemp = 30
        ambientTemp = 25
        engineOilTemp = 90
        throttlePosition = 0.0
        engineLoad = 15.0
        absoluteLoadValue = 15.0
        relativeThrottle = 0.0
        fuelLevel = 75.0
        fuelType = 0x01
        ethanolPercent = 0.0
        fuelPressure = 325
        intakeManifoldPressure = 40
        barometricPressure = 101
        evapSystemVaporPressure = 0
        timingAdvance = 10.0
        mafRate = 2.5
        fuelInjectionTiming = 0.0
        engineFuelRate = 2.5
        oxygenSensor1 = 0.85
        oxygenSensor2 = 0.72
        oxygenSensor3 = 0.90
        oxygenSensor4 = 0.65
        fuelTrimShortTerm1 = 0.0
        fuelTrimShortTerm2 = 0.0
        fuelTrimLongTerm1 = 0.0
        fuelTrimLongTerm2 = 0.0
        controlModuleVoltage = 13.8
        absoluteThrottleB = 0.0
        absoluteThrottleC = 0.0
        acceleratorPedalD = 0.0
        acceleratorPedalE = 0.0
        acceleratorPedalF = 0.0
        commandedEgr = 5.0
        egrError = 0.0
        evaporativePurge = 25.0
        warmUpsSinceCodesCleared = 127
        distanceSinceCodesCleared = 15234
        runtime = 1200
        timeWithMilOn = 0
        timeSinceCodesCleared = 120
        catalystTempBank1Sensor1 = 600.0
        catalystTempBank2Sensor1 = 580.0
        fuelSystemStatus = 0x02
        dtcs.clear()
        pendingDtcs.clear()
    }

    fun update() {
        engineRpm += Random.nextInt(-50, 51)
        engineRpm = engineRpm.coerceIn(600.0, 7000.0)

        vehicleSpeed += Random.nextInt(-3, 4)
        vehicleSpeed = vehicleSpeed.coerceIn(0, 255)

        coolantTemp += Random.nextInt(-1, 2)
        coolantTemp = coolantTemp.coerceIn(-40, 215)

        intakeTemp += Random.nextInt(-1, 2)
        intakeTemp = intakeTemp.coerceIn(-40, 215)

        throttlePosition += Random.nextDouble(-2.0, 2.1)
        throttlePosition = throttlePosition.coerceIn(0.0, 100.0)

        engineLoad = 15.0 + (throttlePosition * 0.5) + Random.nextDouble(-5.0, 5.0)
        engineLoad = engineLoad.coerceIn(0.0, 100.0)
        absoluteLoadValue = engineLoad
        relativeThrottle = throttlePosition

        if (Random.nextFloat() < 0.005f) {
            fuelLevel = (fuelLevel - 0.1).coerceAtLeast(0.0)
        }

        runtime += 1
        timeSinceCodesCleared += 1

        mafRate = 2.0 + (engineRpm / 1000.0) * (1 + throttlePosition / 100.0)
        mafRate += Random.nextDouble(-0.3, 0.3)

        timingAdvance = (8.0 + Random.nextDouble(-2.0, 2.0)).coerceIn(-64.0, 63.5)

        fuelPressure = (300 + Random.nextInt(-20, 21)).coerceIn(0, 765)

        intakeManifoldPressure = (40 + (engineRpm / 100) + (throttlePosition * 0.3)).toInt()
            .coerceIn(0, 255)

        controlModuleVoltage = (13.5 + Random.nextDouble(-0.5, 0.6)).coerceIn(0.0, 65.535)

        absoluteThrottleB = throttlePosition * 0.25
        absoluteThrottleC = throttlePosition * 0.2
        acceleratorPedalD = throttlePosition * 0.3
        acceleratorPedalE = throttlePosition * 0.28
        acceleratorPedalF = throttlePosition * 0.26
        evaporativePurge = 25.0 + Random.nextDouble(-3.0, 3.0)
        commandedEgr = 5.0 + Random.nextDouble(-1.0, 1.0)
        egrError = Random.nextDouble(-2.0, 2.0)
        engineFuelRate = 2.5 + (engineRpm / 2000.0) + Random.nextDouble(-0.2, 0.2)
        fuelInjectionTiming = Random.nextDouble(-2.0, 2.0)

        oxygenSensor1 = 0.45 + Random.nextDouble(0.0, 0.8)
        oxygenSensor2 = 0.45 + Random.nextDouble(0.0, 0.8)
        oxygenSensor3 = 0.45 + Random.nextDouble(0.0, 0.8)
        oxygenSensor4 = 0.45 + Random.nextDouble(0.0, 0.8)

        fuelTrimShortTerm1 = Random.nextDouble(-10.0, 10.0)
        fuelTrimShortTerm2 = Random.nextDouble(-10.0, 10.0)
        fuelTrimLongTerm1 = Random.nextDouble(-10.0, 10.0)
        fuelTrimLongTerm2 = Random.nextDouble(-10.0, 10.0)

        catalystTempBank1Sensor1 =
            400.0 + (engineRpm / 10.0) + (throttlePosition * 2.0) + Random.nextDouble(-20.0, 20.0)
        catalystTempBank2Sensor1 = catalystTempBank1Sensor1 - Random.nextDouble(10.0, 50.0)
    }

    fun getMode01Response(pid: String): String? {
        val code = pid.toIntOrNull(16) ?: return null
        if (code % 0x20 == 0) {
            val nextRangeEmpty = modeledPids.none { it > code && it <= code + 0x20 }
            if (nextRangeEmpty && code != 0x00) return null
            return ObdEncoder.supportedPidBitmap(code, advertisedPids)
        }
        if (code !in modeledPids) return null
        return when (code) {
            0x01, 0x41 -> monitorStatus()
            0x03 -> ObdEncoder.byte(fuelSystemStatus) + " " + ObdEncoder.byte(0x00)
            0x04 -> ObdEncoder.percent8bit(engineLoad)
            0x05 -> ObdEncoder.temperature(coolantTemp)
            0x06 -> ObdEncoder.fuelTrim(fuelTrimShortTerm1)
            0x07 -> ObdEncoder.fuelTrim(fuelTrimLongTerm1)
            0x08 -> ObdEncoder.fuelTrim(fuelTrimShortTerm2)
            0x09 -> ObdEncoder.fuelTrim(fuelTrimLongTerm2)
            0x0A -> ObdEncoder.byte(fuelPressure / 3)
            0x0B -> ObdEncoder.byte(intakeManifoldPressure)
            0x0C -> ObdEncoder.rpm(engineRpm)
            0x0D -> ObdEncoder.speed(vehicleSpeed)
            0x0E -> ObdEncoder.timingAdvance(timingAdvance)
            0x0F -> ObdEncoder.temperature(intakeTemp)
            0x10 -> ObdEncoder.maf(mafRate)
            0x11 -> ObdEncoder.percent8bit(throttlePosition)
            0x14 -> ObdEncoder.o2Sensor(oxygenSensor1)
            0x15 -> ObdEncoder.o2Sensor(oxygenSensor2)
            0x16 -> ObdEncoder.o2Sensor(oxygenSensor3)
            0x17 -> ObdEncoder.o2Sensor(oxygenSensor4)
            0x1C -> ObdEncoder.byte(0x01)
            0x1F -> ObdEncoder.runtime(runtime)
            0x21 -> ObdEncoder.distance(timeWithMilOn)
            0x23 -> ObdEncoder.railPressureGauge(fuelPressure.toDouble())
            0x2C -> ObdEncoder.percent8bit(commandedEgr)
            0x2D -> ObdEncoder.fuelTrim(egrError)
            0x2E -> ObdEncoder.percent8bit(evaporativePurge)
            0x2F -> ObdEncoder.percent8bit(fuelLevel)
            0x30 -> ObdEncoder.byte(warmUpsSinceCodesCleared)
            0x31 -> ObdEncoder.distance(distanceSinceCodesCleared)
            0x32 -> ObdEncoder.evapVaporPressure(evapSystemVaporPressure)
            0x33 -> ObdEncoder.byte(barometricPressure)
            0x3C -> ObdEncoder.catalystTemperature(catalystTempBank1Sensor1)
            0x3D -> ObdEncoder.catalystTemperature(catalystTempBank1Sensor1 * 0.98)
            0x3E -> ObdEncoder.catalystTemperature(catalystTempBank2Sensor1)
            0x42 -> ObdEncoder.voltage(controlModuleVoltage)
            0x43 -> ObdEncoder.absoluteLoad(absoluteLoadValue)
            0x44 -> ObdEncoder.equivalenceRatio(1.0)
            0x45 -> ObdEncoder.percent8bit(relativeThrottle)
            0x46 -> ObdEncoder.temperature(ambientTemp)
            0x47 -> ObdEncoder.percent8bit(absoluteThrottleB)
            0x48 -> ObdEncoder.percent8bit(absoluteThrottleC)
            0x49 -> ObdEncoder.percent8bit(acceleratorPedalD)
            0x4A -> ObdEncoder.percent8bit(acceleratorPedalE)
            0x4B -> ObdEncoder.percent8bit(acceleratorPedalF)
            0x4C -> ObdEncoder.percent8bit(throttlePosition)
            0x4D -> ObdEncoder.runtime(timeWithMilOn)
            0x4E -> ObdEncoder.runtime(timeSinceCodesCleared / 60)
            0x51 -> ObdEncoder.byte(fuelType)
            0x52 -> ObdEncoder.percent8bit(ethanolPercent)
            0x5C -> ObdEncoder.temperature(engineOilTemp)
            0x5D -> ObdEncoder.injectionTiming(fuelInjectionTiming)
            0x5E -> ObdEncoder.word((engineFuelRate * 20).toInt())
            else -> null
        }
    }

    /**
     * Mode 02 (freeze frame). The payload starts with the frame number, then
     * the same encoding as mode 01.
     */
    fun getMode02Response(pid: String, frame: Int = 0): String? {
        val code = pid.toIntOrNull(16) ?: return null
        val frameByte = ObdEncoder.byte(frame)
        val data = when (code) {
            // PID 0x02 in mode 02 is the DTC that stored the freeze frame.
            0x02 -> "00 00"
            0x04 -> ObdEncoder.percent8bit(engineLoad)
            0x05 -> ObdEncoder.temperature(coolantTemp)
            0x06 -> ObdEncoder.fuelTrim(fuelTrimShortTerm1)
            0x07 -> ObdEncoder.fuelTrim(fuelTrimLongTerm1)
            0x0B -> ObdEncoder.byte(intakeManifoldPressure)
            0x0C -> ObdEncoder.rpm(engineRpm)
            0x0D -> ObdEncoder.speed(vehicleSpeed)
            0x0F -> ObdEncoder.temperature(intakeTemp)
            0x10 -> ObdEncoder.maf(mafRate)
            0x11 -> ObdEncoder.percent8bit(throttlePosition)
            0x14 -> ObdEncoder.o2Sensor(oxygenSensor1)
            0x1F -> ObdEncoder.runtime(runtime)
            0x21 -> ObdEncoder.distance(timeWithMilOn)
            0x2F -> ObdEncoder.percent8bit(fuelLevel)
            0x42 -> ObdEncoder.voltage(controlModuleVoltage)
            else -> return null
        }
        return "$frameByte $data"
    }

    fun getMode03Response(): String {
        return if (dtcs.isEmpty()) "00 00 00 00" else dtcs.joinToString(" ")
    }

    fun getMode07Response(): String {
        return if (pendingDtcs.isEmpty()) "00 00 00 00" else pendingDtcs.joinToString(" ")
    }

    fun getMode0AResponse(): String {
        return "00 00 00 00"
    }

    /** ATRV: measured battery voltage. */
    fun getBatteryVoltage(): Double = controlModuleVoltage

    /** Read-only accessors so tests can pin the scaling of temperature PIDs. */
    fun getIntakeAirTemperature(): Int = intakeTemp

    fun getAmbientAirTemperature(): Int = ambientTemp

    fun getEngineOilTemperature(): Int = engineOilTemp

    fun getCatalystTemperature(): Double = catalystTempBank1Sensor1

    fun getEngineSpeed(): Double = engineRpm

    fun getVehicleSpeed(): Int = vehicleSpeed

    /** All 17 VIN characters as hex bytes; padding would drop the last digit. */
    fun getVin(): String {
        return vin.map { ObdEncoder.byte(it.code) }.joinToString(" ")
    }

    fun getCalibrationId(): String {
        return "45 4C 4D 33 32 37 00 00 00 00 00 00 00 00 00 00"
    }

    fun resetFreezeFrame() {
        timeWithMilOn = 0
    }

    private fun monitorStatus(): String {
        val a = if (dtcs.isEmpty()) 0x00 else (0x80 or (dtcs.size and 0x7F))
        return ObdEncoder.byte(a) + " " + ObdEncoder.byte(0x07) + " " +
            ObdEncoder.byte(0x65) + " " + ObdEncoder.byte(0x00)
    }
}
