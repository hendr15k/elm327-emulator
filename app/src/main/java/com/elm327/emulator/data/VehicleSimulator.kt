package com.elm327.emulator.data

import kotlin.random.Random

object VehicleSimulator {

    private var engineRpm = 850
    private var vehicleSpeed = 0
    private var coolantTemp = 80
    private var intakeTemp = 30
    private var throttlePosition = 0.0
    private var engineLoad = 15.0
    private var fuelLevel = 75.0
    private var mafRate = 2.5
    private var runtime = 1200
    private var fuelPressure = 325
    private var intakeManifoldPressure = 40
    private var timingAdvance = 10.0
    private var mafAirFlow = 25.0
    private var throttleThrottle = 0.0
    private var oxygenSensor1 = 0.85
    private var oxygenSensor2 = 0.72
    private var oxygenSensor3 = 0.90
    private var oxygenSensor4 = 0.65
    private var runTimeSinceEngineStart = 120
    private var dtcs = mutableListOf<String>()
    private var pendingDtcs = mutableListOf<String>()
    private var fuelSystemStatus = "01"
    private var controlModuleVoltage = 13.8
    private var absoluteThrottleB = 0.0
    private var absoluteThrottleC = 0.0
    private var acceleratorPedalD = 0.0
    private var acceleratorPedalE = 0.0
    private var acceleratorPedalF = 0.0
    private var commandedEgr = 5.0
    private var egrError = 0.0
    private var evaporativePurge = 25.0
    private var fuelLevelInput = 75.0
    private var warmUpsSinceCodesCleared = 127
    private var distanceSinceCodesCleared = 15234
    private var evapSystemVaporPressure = 0
    private var barometricPressure = 101
    private var catalyticConverterTemp = 600.0
    private var catalystTempBank1Sensor1 = 600.0
    private var catalystTempBank2Sensor1 = 580.0
    private var moduleVoltage = 13.8
    private var absoluteLoadValue = 15.0
    private var relativeThrottle = 0.0
    private var ambientTemp = 25
    private var absoluteThrottlePosB = 0.0
    private var absoluteThrottlePosE = 0.0
    private var relativeThrottlePos = 0.0
    private var engineCoolantTemp2 = 75
    private var intakeAirTemp2 = 28
    private var fuelInjectionTiming = 0
    private var engineFuelRate = 2.5
    private var hybridBattery = 75
    private var engineOilTemp = 90
    private var fuelTrimShortTerm1 = 0.0
    private var fuelTrimShortTerm2 = 0.0
    private var fuelTrimLongTerm1 = 0.0
    private var fuelTrimLongTerm2 = 0.0
    private var fuelTrimShortTerm3 = 0.0
    private var fuelTrimShortTerm4 = 0.0
    private var fuelTrimLongTerm3 = 0.0
    private var fuelTrimLongTerm4 = 0.0
    private var o2SensorSWR1 = 0.0
    private var o2SensorSWR2 = 0.0
    private var o2SensorSWR3 = 0.0
    private var o2SensorSWR4 = 0.0
    private var o2SensorSWR5 = 0.0
    private var o2SensorSWR6 = 0.0
    private var o2SensorSWR7 = 0.0
    private var o2SensorSWR8 = 0.0
    private var pidsSupported01 = listOf("04", "05", "06", "07", "0B", "0C", "0D", "0E", "0F", "10", "11", "14", "1F", "21", "2F", "32", "33", "34", "35", "36", "37", "38", "39", "3A", "3B", "3C", "3D", "3E", "3F", "42", "43", "44", "45", "46", "47", "48", "49", "4A", "4B", "4C", "4D", "4E", "4F", "51", "52", "5C", "5D", "5E", "5F", "61", "62", "63", "64", "65", "66", "67", "68", "69", "6A", "6B", "6C", "6D", "6E", "6F", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "7A", "7B", "7C", "7D", "7E", "7F", "81", "82", "83", "84", "85", "86", "87", "88", "89", "8A", "8B", "8C", "8D", "8E", "8F", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", "9A", "9B", "9C", "9D", "9E", "9F", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "AA", "AB", "AC", "AD", "AE", "AF")
    private val vin = "1HGBH41JXMN109186"
    private var freezeFrameData = mutableMapOf<String, String>()

    fun initialize() {
        engineRpm = 850
        vehicleSpeed = 0
        coolantTemp = 80
        intakeTemp = 30
        throttlePosition = 0.0
        engineLoad = 15.0
        fuelLevel = 75.0
        mafRate = 2.5
        runtime = 1200
        dtcs.clear()
        pendingDtcs.clear()
        fuelSystemStatus = "01"
    }

    fun resetFreezeFrame() {
        freezeFrameData.clear()
    }

    fun update() {
        engineRpm += Random.nextInt(-50, 51)
        engineRpm = engineRpm.coerceIn(600, 7000)

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

        if (Random.nextFloat() < 0.005f) {
            fuelLevel -= 0.1
            if (fuelLevel < 0) fuelLevel = 0.0
        }

        runtime += 1

        mafRate = 2.0 + (engineRpm / 1000.0) * (1 + throttlePosition / 100.0)
        mafRate += Random.nextDouble(-0.3, 0.3)

        timingAdvance = 8.0 + Random.nextDouble(-2.0, 2.0)
        timingAdvance = timingAdvance.coerceIn(-64.0, 63.5)

        fuelPressure = 300 + Random.nextInt(-20, 21)
        fuelPressure = fuelPressure.coerceIn(0, 765)

        intakeManifoldPressure = (40 + (engineRpm / 100) + (throttlePosition * 0.3)).toInt()
        intakeManifoldPressure = intakeManifoldPressure.coerceIn(0, 255)

        controlModuleVoltage = 13.5 + Random.nextDouble(-0.5, 0.6)
        controlModuleVoltage = controlModuleVoltage.coerceIn(0.0, 65.535)

        absoluteThrottleB = throttlePosition * 0.25
        acceleratorPedalD = throttlePosition * 0.3
        acceleratorPedalE = throttlePosition * 0.28

        oxygenSensor1 = 0.45 + Random.nextDouble(0.0, 0.8)
        oxygenSensor2 = 0.45 + Random.nextDouble(0.0, 0.8)
        oxygenSensor3 = 0.45 + Random.nextDouble(0.0, 0.8)
        oxygenSensor4 = 0.45 + Random.nextDouble(0.0, 0.8)

        fuelTrimShortTerm1 = Random.nextDouble(-10.0, 10.0)
        fuelTrimShortTerm2 = Random.nextDouble(-10.0, 10.0)
        fuelTrimLongTerm1 = Random.nextDouble(-10.0, 10.0)
        fuelTrimLongTerm2 = Random.nextDouble(-10.0, 10.0)

        absoluteLoadValue = engineLoad
        relativeThrottle = throttlePosition / 100.0 * 100.0

        catalystTempBank1Sensor1 = 400.0 + (engineRpm / 10.0) + (throttlePosition * 2.0) + Random.nextDouble(-20.0, 20.0)
        catalystTempBank2Sensor1 = catalystTempBank1Sensor1 - Random.nextDouble(10.0, 50.0)

        runTimeSinceEngineStart += 1
    }

    fun getMode01Response(pid: String): String? {
        return when (pid.uppercase()) {
            "00" -> buildSupportedPids01()
            "01" -> buildMonitorStatus()
            "02" -> "00 00"
            "03" -> fuelSystemStatus + "00"
            "04" -> calculateEngineLoad()
            "05" -> coolantTemp.toString()
            "06" -> calculateFuelTrim(fuelTrimShortTerm1)
            "07" -> calculateFuelTrim(fuelTrimLongTerm1)
            "08" -> calculateFuelTrim(fuelTrimShortTerm2)
            "09" -> calculateFuelTrim(fuelTrimLongTerm2)
            "0A" -> "%02X".format(fuelPressure / 3)
            "0B" -> ((throttlePosition * 255 / 100).toInt()).toString()
            "0C" -> calculateRpm()
            "0D" -> vehicleSpeed.toString()
            "0E" -> calculateTimingAdvance()
            "0F" -> ((intakeTemp + 40) * (255.0 / 510.0)).toInt().toString()
            "10" -> calculateMaf()
            "11" -> ((throttlePosition * 255 / 100).toInt()).toString()
            "14" -> calculateO2Sensor(oxygenSensor1)
            "15" -> calculateO2Sensor(oxygenSensor2)
            "16" -> calculateO2Sensor(oxygenSensor3)
            "17" -> calculateO2Sensor(oxygenSensor4)
            "1C" -> "01"
            "1F" -> runtime.toString()
            "21" -> calculateRuntimeBytes()
            "22" -> calculateFuelPressure()
            "23" -> calculateControlModuleVoltage()
            "24" -> calculateO2SensorWideband(oxygenSensor1)
            "25" -> calculateO2SensorWideband(oxygenSensor2)
            "26" -> calculateO2SensorWideband(oxygenSensor3)
            "27" -> calculateO2SensorWideband(oxygenSensor4)
            "2F" -> ((fuelLevel * 255 / 100).toInt()).toString()
            "30" -> runTimeSinceEngineStart.toString()
            "31" -> distanceSinceCodesCleared.toString()
            "32" -> calculateEvapPressure()
            "33" -> barometricPressure.toString()
            "34" -> calculateMafAirFlow()
            "35" -> calculateMafAirFlow()
            "36" -> calculateMafAirFlow()
            "37" -> calculateMafAirFlow()
            "38" -> calculateMafAirFlow()
            "39" -> calculateMafAirFlow()
            "3A" -> calculateMafAirFlow()
            "3B" -> calculateMafAirFlow()
            "3C" -> calculateCatalystTemp()
            "3D" -> calculateCatalystTemp()
            "3E" -> calculateCatalystTemp()
            "3F" -> calculateFuelRailPressure()
            "42" -> calculateControlModuleVoltageHex()
            "43" -> calculateAbsoluteLoad()
            "44" -> calculateCommandedEquilRatio()
            "45" -> ((throttlePosition * 255 / 100).toInt()).toString()
            "46" -> (ambientTemp + 40).toString()
            "47" -> ((absoluteThrottleB * 255 / 100).toInt()).toString()
            "48" -> ((absoluteThrottleC * 255 / 100).toInt()).toString()
            "49" -> ((acceleratorPedalD * 255 / 100).toInt()).toString()
            "4A" -> ((acceleratorPedalE * 255 / 100).toInt()).toString()
            "4B" -> ((acceleratorPedalF * 255 / 100).toInt()).toString()
            "4C" -> ((throttlePosition * 255 / 100).toInt()).toString()
            "4D" -> calculateRuntimeBytes()
            "4E" -> (distanceSinceCodesCleared / 4).toString()
            "51" -> "04"
            "52" -> calculateFuelTrim(fuelTrimLongTerm1)
            "53" -> calculateFuelTrim(fuelTrimLongTerm2)
            "54" -> calculateFuelTrim(fuelTrimShortTerm1)
            "55" -> calculateFuelTrim(fuelTrimShortTerm2)
            "56" -> calculateFuelTrim(fuelTrimShortTerm3)
            "57" -> calculateFuelTrim(fuelTrimShortTerm4)
            "58" -> calculateFuelTrim(fuelTrimLongTerm3)
            "59" -> calculateFuelTrim(fuelTrimLongTerm4)
            "5A" -> ((acceleratorPedalD * 255 / 100).toInt()).toString()
            "5C" -> (intakeTemp + 40).toString()
            "5D" -> calculateInjectionTiming()
            "5E" -> calculateEngineFuelRate()
            "5F" -> "00"
            "61" -> ((throttleThrottle * 255 / 100).toInt()).toString()
            "62" -> ((absoluteThrottlePosB * 255 / 100).toInt()).toString()
            "63" -> ((absoluteThrottlePosE * 255 / 100).toInt()).toString()
            "64" -> ((relativeThrottlePos * 255 / 100).toInt()).toString()
            "65" -> ((absoluteThrottlePosB * 255 / 100).toInt()).toString()
            "66" -> calculateControlModuleVoltageHex()
            "67" -> ((absoluteThrottleC * 255 / 100).toInt()).toString()
            "68" -> ((acceleratorPedalD * 255 / 100).toInt()).toString()
            "69" -> ((acceleratorPedalE * 255 / 100).toInt()).toString()
            "6A" -> ((acceleratorPedalF * 255 / 100).toInt()).toString()
            "6B" -> calculateFuelTrim(fuelTrimShortTerm1)
            "6C" -> calculateCommandedThrottle()
            "6D" -> calculateFuelTrim(fuelTrimShortTerm1)
            "6E" -> calculateFuelTrim(fuelTrimShortTerm1)
            "6F" -> calculateEvapPurge()
            "70" -> calculateEvapPurge()
            "71" -> calculateFuelTrim(fuelTrimShortTerm1)
            "72" -> ((absoluteThrottlePosB * 255 / 100).toInt()).toString()
            "73" -> calculateFuelTrim(fuelTrimShortTerm1)
            "74" -> calculateFuelTrim(fuelTrimShortTerm1)
            "75" -> calculateCatalystTemp()
            "76" -> calculateCatalystTemp()
            "77" -> calculateCatalystTemp()
            "78" -> calculateCatalystTemp()
            "79" -> calculateCatalystTemp()
            "7A" -> calculateCatalystTemp()
            "7B" -> calculateCatalystTemp()
            "7C" -> calculateCatalystTemp()
            "7D" -> calculateFuelTrim(fuelTrimLongTerm1)
            "7E" -> ((throttlePosition * 255 / 100).toInt()).toString()
            "7F" -> calculateEvapPurge()
            "81" -> "00 00 00 00 00 00 00"
            "82" -> "00 00 00 00 00 00 00"
            "83" -> calculateControlModuleVoltageHex()
            "84" -> ((throttleThrottle * 255 / 100).toInt()).toString()
            "85" -> calculateCatalystTemp()
            "86" -> calculateCatalystTemp()
            "87" -> calculateCatalystTemp()
            "88" -> calculateControlModuleVoltageHex()
            "89" -> calculateControlModuleVoltageHex()
            "8A" -> calculateControlModuleVoltageHex()
            "8B" -> calculateControlModuleVoltageHex()
            "8C" -> calculateControlModuleVoltageHex()
            "8D" -> calculateControlModuleVoltageHex()
            "8E" -> calculateControlModuleVoltageHex()
            "8F" -> calculateControlModuleVoltageHex()
            "90" -> calculateControlModuleVoltageHex()
            "91" -> calculateControlModuleVoltageHex()
            "92" -> calculateControlModuleVoltageHex()
            "93" -> calculateControlModuleVoltageHex()
            "94" -> calculateControlModuleVoltageHex()
            "95" -> calculateControlModuleVoltageHex()
            "96" -> calculateControlModuleVoltageHex()
            "97" -> calculateControlModuleVoltageHex()
            "98" -> calculateControlModuleVoltageHex()
            "99" -> calculateControlModuleVoltageHex()
            "9A" -> calculateControlModuleVoltageHex()
            "9B" -> calculateControlModuleVoltageHex()
            "9C" -> calculateControlModuleVoltageHex()
            "9D" -> calculateControlModuleVoltageHex()
            "9E" -> calculateControlModuleVoltageHex()
            "9F" -> calculateControlModuleVoltageHex()
            "A1" -> "00 00 00 00 00 00 00 00"
            "A2" -> "00 00 00 00 00 00 00 00"
            "A3" -> "00 00 00 00 00 00 00 00"
            "A4" -> calculateControlModuleVoltageHex()
            "A5" -> calculateControlModuleVoltageHex()
            "A6" -> calculateControlModuleVoltageHex()
            "A7" -> calculateControlModuleVoltageHex()
            "A8" -> calculateControlModuleVoltageHex()
            "A9" -> calculateControlModuleVoltageHex()
            "AA" -> calculateControlModuleVoltageHex()
            "AB" -> calculateControlModuleVoltageHex()
            "AC" -> calculateControlModuleVoltageHex()
            "AD" -> calculateControlModuleVoltageHex()
            "AE" -> calculateControlModuleVoltageHex()
            "AF" -> calculateControlModuleVoltageHex()
            else -> null
        }
    }

    fun getMode02Response(pid: String): String? {
        return when (pid.uppercase()) {
            "02" -> {
                val storedCoolant = (coolantTemp - 40).coerceIn(-40, 215)
                "%02X".format(storedCoolant) + "00"
            }
            "04" -> ((engineLoad * 255 / 100).toInt()).toString() + "00"
            "06" -> calculateFuelTrim(fuelTrimShortTerm1) + "00"
            "07" -> calculateFuelTrim(fuelTrimLongTerm1) + "00"
            "0D" -> ((vehicleSpeed - 1).coerceAtLeast(0)).toString()
            "0F" -> ((intakeTemp - 40) * (255.0 / 510.0).toInt()).toString() + "00"
            "10" -> calculateMaf()
            "14" -> calculateO2Sensor(oxygenSensor1) + "00"
            "1F" -> ((runtime - 1).coerceAtLeast(0)).toString()
            "21" -> ((runtime - 1) / 256).toString() + " " + ((runtime - 1) % 256).toString()
            "2F" -> ((fuelLevel - 1).coerceAtLeast(0.0) * 255 / 100).toInt().toString() + "00"
            else -> null
        }
    }

    fun getMode03Response(): String {
        return if (dtcs.isEmpty()) "00 00 00 00" else dtcs.joinToString(" ")
    }

    fun getMode07Response(): String {
        return if (pendingDtcs.isEmpty()) "00 00 00 00" else pendingDtcs.joinToString(" ")
    }

    fun getVin(): String {
        val vinBytes = vin.map { "%02X".format(it.code) }.chunked(2).map {
            if (it.size == 2) it.joinToString(" ") else "00"
        }
        val padded = vinBytes.toMutableList()
        while (padded.size < 9) padded.add("00")
        return padded.take(9).joinToString(" ")
    }

    private fun buildSupportedPids01(): String {
        val bits = StringBuilder("00".repeat(4))
        val supported = listOf("00","01","02","03","04","05","06","07","08","09","0A","0B","0C","0D","0E","0F",
            "10","11","12","13","14","15","16","17","18","19","1A","1B","1C","1D","1E","1F",
            "20","21","22","23","24","25","26","27","28","29","2A","2B","2C","2D","2E","2F",
            "30","31","32","33","34","35","36","37","38","39","3A","3B","3C","3D","3E","3F",
            "40","41","42","43","44","45","46","47","48","49","4A","4B","4C","4D","4E","4F",
            "50","51","52","53","54","55","56","57","58","59","5A","5B","5C","5D","5E","5F",
            "60","61","62","63","64","65","66","67","68","69","6A","6B","6C","6D","6E","6F",
            "70","71","72","73","74","75","76","77","78","79","7A","7B","7C","7D","7E","7F",
            "80","81","82","83","84","85","86","87","88","89","8A","8B","8C","8D","8E","8F",
            "90","91","92","93","94","95","96","97","98","99","9A","9B","9C","9D","9E","9F",
            "A0","A1","A2","A3","A4","A5","A6","A7","A8","A9","AA","AB","AC","AD","AE","AF")
        val byteIndex = 0
        val byteValue = supported.filter { it.toInt(16) < 32 }.size
        return "%02X".format(byteValue) + " 3E A8 13"
    }

    private fun buildMonitorStatus(): String {
        return "01 00 00 00"
    }

    private fun calculateRpm(): String {
        val rpmValue = (engineRpm / 4)
        val a = rpmValue / 256
        val b = rpmValue % 256
        return "%02X %02X".format(a, b)
    }

    private fun calculateEngineLoad(): String {
        val load = (engineLoad * 255 / 100).toInt()
        return "%02X".format(load)
    }

    private fun calculateMaf(): String {
        val maf = (mafRate * 100).toInt()
        val a = maf / 256
        val b = maf % 256
        return "%02X %02X".format(a, b)
    }

    private fun calculateTimingAdvance(): String {
        val advance = ((timingAdvance + 128) * 2).toInt()
        return "%02X".format(advance.coerceIn(0, 255))
    }

    private fun calculateFuelTrim(value: Double): String {
        val offset = ((value + 100) * 128 / 100).toInt()
        return "%02X".format(offset.coerceIn(0, 255))
    }

    private fun calculateO2Sensor(voltage: Double): String {
        val voltageByte = (voltage * 200).toInt().coerceIn(0, 255)
        val trimByte = 128
        return "%02X %02X".format(voltageByte, trimByte)
    }

    private fun calculateO2SensorWideband(voltage: Double): String {
        val voltageByte = (voltage * 2048 / 5).toInt().coerceIn(0, 65535)
        val a = voltageByte / 256
        val b = voltageByte % 256
        val trimByte = 128
        return "%02X %02X %02X".format(a, b, trimByte)
    }

    private fun calculateRuntimeBytes(): String {
        val a = runtime / 256
        val b = runtime % 256
        return "$a $b"
    }

    private fun calculateFuelPressure(): String {
        val pressure = (fuelPressure / 3).toInt()
        return "%02X %02X".format(pressure / 256, pressure % 256)
    }

    private fun calculateControlModuleVoltage(): String {
        return "%02X".format((controlModuleVoltage * 4).toInt().coerceIn(0, 255))
    }

    private fun calculateControlModuleVoltageHex(): String {
        val voltage = (controlModuleVoltage * 1000).toInt()
        val a = voltage / 256
        val b = voltage % 256
        return "%02X %02X".format(a, b)
    }

    private fun calculateMafAirFlow(): String {
        val flow = (mafAirFlow * 100).toInt()
        val a = flow / 256
        val b = flow % 256
        return "%02X %02X".format(a, b)
    }

    private fun calculateCatalystTemp(): String {
        val temp = (catalystTempBank1Sensor1 + 40) * 100 / 256
        val a = temp.toInt() / 256
        val b = temp.toInt() % 256
        return "%02X %02X".format(a, b)
    }

    private fun calculateEvapPressure(): String {
        val pressure = ((evapSystemVaporPressure + 32767) * 4).toInt()
        val a = pressure / 256
        val b = pressure % 256
        return "%02X %02X".format(a, b)
    }

    private fun calculateFuelRailPressure(): String {
        val pressure = (fuelPressure * 10).toInt()
        val a = pressure / 256
        val b = pressure % 256
        return "%02X %02X".format(a, b)
    }

    private fun calculateAbsoluteLoad(): String {
        val load = (absoluteLoadValue * 255 / 100).toInt()
        return "%02X %02X".format(load / 256, load % 256)
    }

    private fun calculateCommandedEquilRatio(): String {
        val ratio = (2.0 / 128.0 * 65536).toInt()
        val a = ratio / 256
        val b = ratio % 256
        return "%02X %02X".format(a, b)
    }

    private fun calculateInjectionTiming(): String {
        val timing = ((fuelInjectionTiming + 210) * 128 / 50).toInt()
        return "%02X %02X".format(timing / 256, timing % 256)
    }

    private fun calculateEngineFuelRate(): String {
        val rate = (engineFuelRate * 20).toInt()
        return "%02X %02X".format(rate / 256, rate % 256)
    }

    private fun calculateEvapPurge(): String {
        return "%02X".format(evaporativePurge.toInt().coerceIn(0, 255))
    }

    private fun calculateCommandedThrottle(): String {
        return "%02X".format(throttlePosition.toInt().coerceIn(0, 255))
    }
}
