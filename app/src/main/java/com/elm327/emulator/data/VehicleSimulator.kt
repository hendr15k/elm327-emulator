package com.elm327.emulator.data

import kotlin.random.Random

object VehicleSimulator {

    private var engineRpm = 850
    private var vehicleSpeed = 0
    private var coolantTemp = 80
    private var throttlePosition = 0.0
    private var fuelLevel = 75.0
    private var intakeTemp = 30
    private var mafRate = 2.5
    private var runtime = 1200
    private var dtcs = mutableListOf<String>()

    fun initialize() {
        engineRpm = 850
        vehicleSpeed = 0
        coolantTemp = 80
        throttlePosition = 0.0
        fuelLevel = 75.0
        intakeTemp = 30
        mafRate = 2.5
        runtime = 1200
        dtcs.clear()
    }

    fun update() {
        engineRpm += Random.nextInt(-50, 51)
        engineRpm = engineRpm.coerceIn(600, 7000)

        vehicleSpeed += Random.nextInt(-5, 6)
        vehicleSpeed = vehicleSpeed.coerceIn(0, 255)

        coolantTemp += Random.nextInt(-1, 2)
        coolantTemp = coolantTemp.coerceIn(-40, 215)

        throttlePosition += Random.nextDouble(-1.0, 1.1)
        throttlePosition = throttlePosition.coerceIn(0.0, 100.0)

        if (Random.nextFloat() < 0.01f) {
            fuelLevel -= 0.1
            if (fuelLevel < 0) fuelLevel = 0.0
        }

        intakeTemp += Random.nextInt(-1, 2)
        intakeTemp = intakeTemp.coerceIn(-40, 215)

        runtime += 1

        mafRate = 2.0 + (engineRpm / 1000.0) * (1 + throttlePosition / 100.0)
    }

    fun getMode01Response(pid: String): String? {
        return when (pid.uppercase()) {
            "00" -> "BE 3E A8 13"
            "01" -> "00 00 00 00"
            "04" -> calculateEngineLoad()
            "05" -> coolantTemp.toString()
            "06" -> "00"
            "07" -> "00"
            "0B" -> ((throttlePosition * 255 / 100).toInt()).toString()
            "0C" -> calculateRpm()
            "0D" -> vehicleSpeed.toString()
            "0E" -> "00"
            "0F" -> ((intakeTemp + 40) * (255.0 / 510.0)).toInt().toString()
            "10" -> calculateMaf()
            "11" -> ((throttlePosition * 255 / 100).toInt()).toString()
            "14" -> "00 00"
            "1F" -> runtime.toString()
            "20" -> "00 00 00 00"
            "21" -> (runtime / 256).toString() + " " + (runtime % 256).toString()
            "2F" -> ((fuelLevel * 255 / 100).toInt()).toString()
            "33" -> "00"
            "40" -> "00 00 00 00"
            "42" -> "00 00"
            "46" -> "00"
            "4D" -> (runtime / 256).toString() + " " + (runtime % 256).toString()
            "4F" -> "00 00 00 00"
            "51" -> "04"
            "5C" -> (intakeTemp + 40).toString()
            "5E" -> calculateEnginePower()
            "60" -> "00 00 00 00"
            "80" -> "00 00 00 00"
            else -> null
        }
    }

    fun getMode03Response(): String {
        return if (dtcs.isEmpty()) "00 00 00 00" else dtcs.joinToString(" ")
    }

    fun getMode07Response(): String {
        return "00 00 00 00"
    }

    private fun calculateRpm(): String {
        val a = (engineRpm / 4) / 256
        val b = (engineRpm / 4) % 256
        return "%02X %02X".format(a, b)
    }

    private fun calculateEngineLoad(): String {
        val load = (throttlePosition * 255 / 100).toInt()
        return "%02X".format(load)
    }

    private fun calculateMaf(): String {
        val maf = (mafRate * 100).toInt()
        val a = maf / 256
        val b = maf % 256
        return "%02X %02X".format(a, b)
    }

    private fun calculateEnginePower(): String {
        val power = runtime
        val a = power / 256
        val b = power % 256
        return "%02X %02X".format(a, b)
    }
}
