package com.elm327.emulator.data

class Elm327Parser {

    private var echoEnabled = true
    private var lineFeedEnabled = false
    private var headersEnabled = false
    private var protocol = "AUTO"
    private var protocolNumber = 0

    fun reset() {
        echoEnabled = true
        lineFeedEnabled = false
        headersEnabled = false
        protocol = "AUTO"
        protocolNumber = 0
        VehicleSimulator.initialize()
    }

    fun parse(command: String): List<String> {
        val trimmed = command.trim().replace("\r", "").replace("\n", "").replace(" ", "")
        if (trimmed.isEmpty()) return listOf(">")

        VehicleSimulator.update()

        if (trimmed.startsWith("AT", ignoreCase = true)) {
            return parseAtCommand(trimmed)
        }

        if (trimmed.length >= 4 && (trimmed.substring(0, 2) == "01" || trimmed.substring(0, 2) == "03" || trimmed.substring(0, 2) == "07")) {
            return parseObdCommand(trimmed)
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
            "ATS0" -> listOf("OK")
            "ATS1" -> listOf("OK")
            "ATSP0" -> {
                protocol = "AUTO"
                protocolNumber = 0
                listOf("OK")
            }
            "ATSP1" -> {
                protocol = "SAE J1850 PWM"
                protocolNumber = 1
                listOf("OK")
            }
            "ATSP2" -> {
                protocol = "SAE J1850 VPW"
                protocolNumber = 2
                listOf("OK")
            }
            "ATSP3" -> {
                protocol = "ISO 9141-2"
                protocolNumber = 3
                listOf("OK")
            }
            "ATSP4" -> {
                protocol = "ISO 14230-4 (KWP2000)"
                protocolNumber = 4
                listOf("OK")
            }
            "ATSP5" -> {
                protocol = "ISO 15765-4 (CAN)"
                protocolNumber = 5
                listOf("OK")
            }
            "ATSP6" -> {
                protocol = "ISO 15765-4 (CAN)"
                protocolNumber = 6
                listOf("OK")
            }
            "ATSP7" -> {
                protocol = "ISO 15765-4 (CAN)"
                protocolNumber = 7
                listOf("OK")
            }
            "ATSP8" -> {
                protocol = "ISO 15765-4 (CAN)"
                protocolNumber = 8
                listOf("OK")
            }
            "ATSP9" -> {
                protocol = "ISO 15765-4 (CAN)"
                protocolNumber = 9
                listOf("OK")
            }
            "ATSTFF" -> listOf("OK")
            "AT@1" -> listOf("OBDLink")
            "AT@0" -> listOf("ELM327")
            "ATI" -> listOf("ELM327 v1.5")
            "ATI0" -> listOf("ELM327")
            "ATI1" -> listOf("ELM327 v1.5")
            "ATRV" -> listOf("12.0V")
            "ATDP" -> listOf(protocol)
            "ATDPN" -> listOf(protocolNumber.toString())
            "ATST" -> listOf("08")
            "ATFE" -> listOf("OK")
            "ATD" -> {
                reset()
                listOf("OK")
            }
            "ATWS" -> {
                reset()
                listOf("ELM327 v1.5")
            }
            "0100" -> {
                listOf("41 00 BE 3E A8 13")
            }
            "0120" -> {
                listOf("41 20 00 00 00 00")
            }
            "0140" -> {
                listOf("41 40 00 00 00 00")
            }
            "0160" -> {
                listOf("41 60 00 00 00 00")
            }
            "0180" -> {
                listOf("41 80 00 00 00 00")
            }
            "0101" -> {
                listOf("41 01 00 00 00 00")
            }
            else -> listOf("?")
        }
    }

    private fun parseObdCommand(cmd: String): List<String> {
        val mode = cmd.substring(0, 2)
        val pid = if (cmd.length >= 4) cmd.substring(2, 4) else ""

        return when (mode) {
            "01" -> {
                val response = VehicleSimulator.getMode01Response(pid)
                if (response != null) {
                    if (headersEnabled) {
                        listOf("41 $pid $response")
                    } else {
                        listOf("41$pid$response")
                    }
                } else {
                    listOf("NO DATA")
                }
            }
            "03" -> {
                listOf("43 ${VehicleSimulator.getMode03Response()}")
            }
            "07" -> {
                listOf("47 ${VehicleSimulator.getMode07Response()}")
            }
            else -> listOf("?")
        }
    }

    fun isEchoEnabled(): Boolean = echoEnabled
    fun isLineFeedEnabled(): Boolean = lineFeedEnabled
    fun isHeadersEnabled(): Boolean = headersEnabled
}
