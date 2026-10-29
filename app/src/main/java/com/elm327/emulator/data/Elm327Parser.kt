package com.elm327.emulator.data

import java.util.Locale

/**
 * ELM327 command interpreter.
 *
 * Replies are assembled the way the real chip formats them: each message ends
 * with a carriage return (plus a line feed when ATL1 is active), the prompt is
 * a CR followed by '>', and byte spacing follows ATS0/ATS1. Leaving that to the
 * caller is what used to glue an echoed command onto its own response.
 */
class Elm327Parser {

    private var echoEnabled = true
    private var lineFeedEnabled = false
    private var headersEnabled = false
    private var spacingEnabled = true
    private var protocolNumber = 0
    private var autoProtocol = true
    private var timeout = 0x32
    private var deviceIdentifier = "ELM327"

    fun reset() {
        echoEnabled = true
        lineFeedEnabled = false
        headersEnabled = false
        spacingEnabled = true
        protocolNumber = 0
        autoProtocol = true
        timeout = 0x32
        deviceIdentifier = "ELM327"
        VehicleSimulator.initialize()
    }

    fun parse(command: String): List<String> {
        val trimmed = command
            .filterNot { it == '\r' || it == '\n' }
            .replace(" ", "")
            .replace("\t", "")
            .trim()

        if (trimmed.isEmpty() || trimmed == ">") {
            return emptyList()
        }

        VehicleSimulator.update()

        if (trimmed.startsWith("AT", ignoreCase = true)) {
            return parseAtCommand(trimmed)
        }

        if (trimmed.length < 2) return listOf("?")

        return when (trimmed.substring(0, 2).uppercase(Locale.US)) {
            "01" -> if (trimmed.length >= 4) parseMode01(trimmed) else listOf("?")
            "02" -> if (trimmed.length >= 4) parseMode02(trimmed) else listOf("?")
            "03" -> listOf(message(0x43, "", VehicleSimulator.getMode03Response()))
            "04" -> {
                VehicleSimulator.resetFreezeFrame()
                listOf("44")
            }
            "05" -> listOf(message(0x45, "", "00 00 00 00"))
            "06" -> listOf(message(0x46, "", "00 00 00 00 00 00"))
            "07" -> listOf(message(0x47, "", VehicleSimulator.getMode07Response()))
            "08" -> listOf(message(0x48, "", "00 00 00 00"))
            "09" -> if (trimmed.length >= 4) parseMode09(trimmed) else parseMode09Info()
            "0A" -> listOf(message(0x4A, "", VehicleSimulator.getMode0AResponse()))
            else -> listOf("?")
        }
    }

    /**
     * Full reply for one command line: optional echo, CR terminated messages
     * and the CR '>' prompt.
     */
    fun formatReply(command: String, responses: List<String>): String {
        val eol = if (lineFeedEnabled) "\r\n" else "\r"
        val builder = StringBuilder()
        if (echoEnabled && command.isNotEmpty()) {
            builder.append(command).append(eol)
        }
        for (line in responses) {
            if (line.isEmpty()) continue
            builder.append(line).append(eol)
        }
        builder.append(eol).append('>')
        return builder.toString()
    }

    fun isEchoEnabled() = echoEnabled
    fun isLineFeedEnabled() = lineFeedEnabled
    fun isHeadersEnabled() = headersEnabled
    fun isSpacingEnabled() = spacingEnabled

    private fun parseMode01(command: String): List<String> {
        val pid = command.substring(2, 4).uppercase(Locale.US)
        val data = VehicleSimulator.getMode01Response(pid) ?: return listOf("NO DATA")
        return listOf(message(0x41, pid, data))
    }

    private fun parseMode02(command: String): List<String> {
        val pid = command.substring(2, 4).uppercase(Locale.US)
        val frame = if (command.length >= 6) command.substring(4, 6).toIntOrNull(16) ?: 0 else 0
        val data = VehicleSimulator.getMode02Response(pid, frame) ?: return listOf("NO DATA")
        return listOf(message(0x42, pid, data))
    }

    private fun parseMode09(command: String): List<String> {
        val pid = command.substring(2, 4).uppercase(Locale.US)
        return when (pid) {
            "00" -> listOf(message(0x49, "00", ObdEncoder.supportedPidBitmap(0x00, MODE09_PIDS)))
            "02" -> listOf(message(0x49, "02", "01"))
            "04" -> listOf(message(0x49, "04", "01 " + VehicleSimulator.getVin()))
            "06" -> listOf(message(0x49, "06", "01 " + VehicleSimulator.getCalibrationId()))
            "0A" -> listOf(message(0x49, "0A", "01 45 4C 4D 33 32 37"))
            else -> listOf("NO DATA")
        }
    }

    private fun parseMode09Info(): List<String> {
        return listOf(message(0x49, "00", ObdEncoder.supportedPidBitmap(0x00, MODE09_PIDS)))
    }

    /**
     * Assembles one OBD reply: optional CAN header plus PCI byte when headers
     * are enabled, the service byte, the PID and the payload.
     */
    private fun message(service: Int, pid: String, data: String): String {
        val dataBytes = data.split(' ').filter { it.isNotEmpty() }
        val bytes = ArrayList<String>(dataBytes.size + 4)
        if (headersEnabled) {
            // ISO 15765-4: the length byte counts every following diagnostic
            // byte, i.e. service + PID + payload ("7E8 06 41 00 ...").
            val payloadLength = 1 + (if (pid.isEmpty()) 0 else 1) + dataBytes.size
            bytes.add("7E8")
            bytes.add(ObdEncoder.byte(payloadLength))
        }
        bytes.add(ObdEncoder.byte(service))
        if (pid.isNotEmpty()) bytes.add(pid.uppercase(Locale.US))
        bytes.addAll(dataBytes)
        return if (spacingEnabled) bytes.joinToString(" ") else bytes.joinToString("")
    }

    private fun parseAtCommand(command: String): List<String> {
        val upper = command.uppercase(Locale.US)

        // Parameterised commands come first: an exact-match table cannot see ATST64.
        if (upper.startsWith("ATST") && upper.length > 4) {
            timeout = upper.substring(4).toIntOrNull(16) ?: timeout
            return listOf("OK")
        }
        if (upper.startsWith("ATSP")) {
            return setProtocol(upper.substring(4))
        }
        if (upper.startsWith("AT@3")) {
            deviceIdentifier = command.substring(4).trim().ifEmpty { deviceIdentifier }
            return listOf("OK")
        }
        if (CONFIG_PREFIXES.any { upper.startsWith(it) }) {
            return listOf("OK")
        }

        return when (upper) {
            "ATZ", "ATZ0", "ATZ1", "ATWS" -> {
                reset()
                listOf("ELM327 v1.5")
            }
            "ATD" -> {
                reset()
                listOf("OK")
            }
            "ATFE" -> {
                reset()
                listOf("OK")
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
                spacingEnabled = false
                listOf("OK")
            }
            "ATS1" -> {
                spacingEnabled = true
                listOf("OK")
            }
            "ATI", "ATI1" -> listOf("ELM327 v1.5")
            "ATI0" -> listOf("ELM327")
            "AT@1" -> listOf("OBDLink")
            "AT@2" -> listOf(deviceIdentifier)
            "AT@0" -> {
                deviceIdentifier = "ELM327"
                listOf(deviceIdentifier)
            }
            "ATRV" -> listOf(String.format(Locale.US, "%.1fV", VehicleSimulator.getBatteryVoltage()))
            "ATDP" -> listOf(displayProtocol())
            "ATDPN" -> listOf(displayProtocolNumber())
            "ATIGN" -> listOf("ON")
            "ATMA", "ATMR", "ATMT" -> listOf("NO DATA")
            "ATPPS" -> listOf("00 00 00 00")
            "ATFI" -> listOf("OK")
            else -> listOf("?")
        }
    }

    private fun setProtocol(spec: String): List<String> {
        val auto = spec.startsWith("A")
        val digits = spec.removePrefix("A")
        if (digits.isEmpty() || digits.length > 2) return listOf("?")
        val number = digits.toIntOrNull(16) ?: return listOf("?")
        if (number > 12) return listOf("?")
        protocolNumber = number
        autoProtocol = auto || number == 0
        return listOf("OK")
    }

    private fun displayProtocol(): String {
        val name = PROTOCOL_NAMES.getValue(protocolNumber)
        return if (autoProtocol && protocolNumber != 0) "AUTO, $name" else name
    }

    private fun displayProtocolNumber(): String {
        val code = PROTOCOL_CODES[protocolNumber]
        return if (autoProtocol) "A$code" else code
    }

    private companion object {
        /** Mode 09 PIDs this emulator answers: VIN, calibration id, ECU name. */
        val MODE09_PIDS = setOf(0x02, 0x04, 0x06, 0x0A)

        /** Commands that only change a chip setting this emulator does not model. */
        val CONFIG_PREFIXES = listOf(
            "ATSH", "ATCRA", "ATWM", "ATSR", "ATCV", "ATIB", "ATPB", "ATPP", "ATKW",
            "ATSW", "ATIIA", "ATSI", "ATBI", "ATBD", "ATRD", "ATSD", "ATCAF", "ATCFC",
            "ATDM1", "ATAR", "ATTA", "ATAL", "ATNL", "ATAT", "ATR0", "ATR1", "ATD0", "ATD1"
        )

        /**
         * Protocol numbers as defined in the ELM327 datasheet: 5 is
         * ISO 14230-4 KWP fast init, CAN starts at 6 - not 5.
         */
        val PROTOCOL_NAMES = mapOf(
            0 to "AUTO",
            1 to "SAE J1850 PWM",
            2 to "SAE J1850 VPW",
            3 to "ISO 9141-2",
            4 to "ISO 14230-4 (KWP 5BAUD)",
            5 to "ISO 14230-4 (KWP FAST)",
            6 to "ISO 15765-4 (CAN 11/500)",
            7 to "ISO 15765-4 (CAN 29/500)",
            8 to "ISO 15765-4 (CAN 11/250)",
            9 to "ISO 15765-4 (CAN 29/250)",
            10 to "SAE J1939 (CAN 29/250)",
            11 to "USER1 (CAN 11/125)",
            12 to "USER2 (CAN 11/50)"
        )

        val PROTOCOL_CODES = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C")
    }
}
