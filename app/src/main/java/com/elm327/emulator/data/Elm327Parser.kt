package com.elm327.emulator.data

class Elm327Parser {

    private var echoEnabled = true
    private var lineFeedEnabled = false
    private var headersEnabled = false
    private var protocol = "AUTO"
    private var protocolNumber = 0
    private var timeout = 200
    private var spChars = "/"
    private var deviceIdentifier = "ELM327"

    fun reset() {
        echoEnabled = true
        lineFeedEnabled = false
        headersEnabled = false
        protocol = "AUTO"
        protocolNumber = 0
        timeout = 200
        spChars = "/"
        deviceIdentifier = "ELM327"
        VehicleSimulator.initialize()
    }

    fun parse(command: String): List<String> {
        val trimmed = command.trim()
            .replace("\r", "")
            .replace("\n", "")
            .replace(" ", "")
            .replace("\t", "")

        if (trimmed.isEmpty() || trimmed == ">") {
            return emptyList()
        }

        VehicleSimulator.update()

        if (trimmed.startsWith("AT", ignoreCase = true)) {
            return parseAtCommand(trimmed)
        }

        if (trimmed.length >= 2) {
            val mode = trimmed.substring(0, 2)
            return when (mode) {
                "01" -> {
                    if (trimmed.length >= 4) parseObdMode01(trimmed)
                    else listOf("?")
                }
                "02" -> {
                    if (trimmed.length >= 4) parseObdMode02(trimmed)
                    else listOf("?")
                }
                "03" -> parseObdMode03()
                "04" -> {
                    VehicleSimulator.resetFreezeFrame()
                    listOf("44")
                }
                "05" -> listOf("54 00 00 00 00")
                "06" -> listOf("64 00 00 00 00 00 00")
                "07" -> parseObdMode07()
                "08" -> listOf("84 00 00 00 00 00 00")
                "09" -> {
                    if (trimmed.length >= 4) parseObdMode09(trimmed)
                    else parseObdMode09Info()
                }
                else -> listOf("?")
            }
        }

        return listOf("?")
    }

    private fun parseAtCommand(cmd: String): List<String> {
        return when (cmd.uppercase()) {
            "ATZ" -> {
                reset()
                listOf("ELM327 v1.5")
            }
            "ATE0" -> {
                echoEnabled = false
                listOf("OK")
            }
            "ATE1" -> {
                echoEnabled = true
                listOf("OK")
            }
            "ATL0" -> {
                lineFeedEnabled = false
                listOf("OK")
            }
            "ATL1" -> {
                lineFeedEnabled = true
                listOf("OK")
            }
            "ATH0" -> {
                headersEnabled = false
                listOf("OK")
            }
            "ATH1" -> {
                headersEnabled = true
                listOf("OK")
            }
            "ATS0" -> {
                spChars = " "
                listOf("OK")
            }
            "ATS1" -> {
                spChars = "/"
                listOf("OK")
            }
            "ATSP0" -> {
                protocol = "AUTO"
                protocolNumber = 0
                listOf("OK")
            }
            "ATSP1" -> { protocol = "SAE J1850 PWM"; protocolNumber = 1; listOf("OK") }
            "ATSP2" -> { protocol = "SAE J1850 VPW"; protocolNumber = 2; listOf("OK") }
            "ATSP3" -> { protocol = "ISO 9141-2"; protocolNumber = 3; listOf("OK") }
            "ATSP4" -> { protocol = "ISO 14230-4 (KWP2000)"; protocolNumber = 4; listOf("OK") }
            "ATSP5", "ATSP6", "ATSP7", "ATSP8", "ATSP9" -> {
                protocol = "ISO 15765-4 (CAN)"
                protocolNumber = cmd.last().digitToIntOrNull() ?: 5
                listOf("OK")
            }
            "ATST" -> {
                if (cmd.length >= 4) {
                    timeout = cmd.substring(3).toIntOrNull(16) ?: 200
                }
                listOf("%02X".format(timeout))
            }
            "ATAT0" -> { listOf("OK") }
            "ATAT1" -> { listOf("OK") }
            "ATAT2" -> { listOf("OK") }
            "ATSTFF" -> {
                timeout = 255
                listOf("OK")
            }
            "AT@1" -> listOf("OBDLink")
            "AT@0" -> {
                deviceIdentifier = "ELM327"
                listOf(deviceIdentifier)
            }
            "ATI" -> listOf("ELM327 v1.5")
            "ATI0" -> listOf("ELM327")
            "ATI1" -> listOf("ELM327 v1.5")
            "ATRV" -> listOf("12.0V")
            "ATDP" -> listOf(protocol)
            "ATDPN" -> listOf(protocolNumber.toString())
            "ATD" -> { reset(); listOf("OK") }
            "ATWS" -> { reset(); listOf("ELM327 v1.5") }
            "ATFE" -> { reset(); listOf("OK") }
            "ATR0" -> listOf("OK")
            "ATR1" -> listOf("OK")
            "ATSPA0" -> { protocol = "AUTO"; protocolNumber = 0; listOf("OK") }
            "ATSPA1" -> { protocol = "SAE J1850 PWM"; protocolNumber = 1; listOf("OK") }
            "ATSPA2" -> { protocol = "SAE J1850 VPW"; protocolNumber = 2; listOf("OK") }
            "ATSPA3" -> { protocol = "ISO 9141-2"; protocolNumber = 3; listOf("OK") }
            "ATSPA4" -> { protocol = "ISO 14230-4 (KWP2000)"; protocolNumber = 4; listOf("OK") }
            "ATSPA5" -> { protocol = "ISO 15765-4 (CAN)"; protocolNumber = 5; listOf("OK") }
            "ATFI" -> listOf("?")
            "ATPP00" -> listOf("OK")
            "ATPP01" -> listOf("OK")
            "ATPP02" -> listOf("OK")
            "ATPP03" -> listOf("OK")
            "ATPPS" -> listOf("00 00 00 00")
            else -> listOf("?")
        }
    }

    private fun parseObdMode01(cmd: String): List<String> {
        val pid = if (cmd.length >= 4) cmd.substring(2, 4) else ""
        val response = VehicleSimulator.getMode01Response(pid)
        return if (response != null) {
            if (headersEnabled) {
                listOf("41 $pid $response")
            } else {
                listOf("41$pid$response")
            }
        } else {
            listOf("NO DATA")
        }
    }

    private fun parseObdMode02(cmd: String): List<String> {
        val pid = if (cmd.length >= 4) cmd.substring(2, 4) else ""
        val response = VehicleSimulator.getMode02Response(pid)
        return if (response != null) {
            if (headersEnabled) {
                listOf("42 $pid $response")
            } else {
                listOf("42$pid$response")
            }
        } else {
            listOf("NO DATA")
        }
    }

    private fun parseObdMode03(): List<String> {
        val dtcs = VehicleSimulator.getMode03Response()
        return listOf("43 $dtcs")
    }

    private fun parseObdMode07(): List<String> {
        val dtcs = VehicleSimulator.getMode07Response()
        return listOf("47 $dtcs")
    }

    private fun parseObdMode09(pid: String): List<String> {
        val subPid = if (pid.length >= 4) pid.substring(2, 4) else ""
        return when (subPid) {
            "02" -> listOf("49 02 00 00 00 00 00 00 00")
            "04" -> listOf("49 04 ${VehicleSimulator.getVin()}")
            "06" -> listOf("49 06 00 00 00 00 00 00 00")
            "08" -> listOf("49 08 00 00 00 00 00 00 00")
            "0A" -> listOf("49 0A 45 4C 4D 33 32 37 00 00 00 00 00")
            else -> listOf("NO DATA")
        }
    }

    private fun parseObdMode09Info(): List<String> {
        return listOf("49 00 01 05 01 00")
    }

    fun isEchoEnabled() = echoEnabled
    fun isLineFeedEnabled() = lineFeedEnabled
    fun isHeadersEnabled() = headersEnabled
}
